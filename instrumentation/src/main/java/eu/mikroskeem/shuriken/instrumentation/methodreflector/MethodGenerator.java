package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.instrumentation.ClassTools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.mikroskeem.shuriken.instrumentation.Descriptor.newDescriptor;
import static org.objectweb.asm.Opcodes.*;


/**
 * @author Mark Vainomaa
 */
final class MethodGenerator {
    /* Field names */
    final static String REFF = "ref";
    final static String MHF = "mh";

    /* Common types */
    final static Type MH = Type.getType(MethodHandle.class);
    final static Type MH_ARRAY = Type.getType(MethodHandle[].class);
    final static Type OBJECT = Type.getType(Object.class);

    /* Generates class base */
    static void generateClassBase(ClassVisitor cv,
                                  boolean useInstance, boolean useMH, boolean isTargetPublic,
                                  Type proxyClass, Type targetClass) {
        FieldVisitor fv;
        GeneratorAdapter adapter;
        MethodVisitor mv;

        if(useMH || useInstance) {
            /* Use java.lang.Object instead */
            if(!isTargetPublic) {
                targetClass = OBJECT;
            }

            /* Figure out what descriptor to use */
            String descriptor;
            if(useInstance && useMH) {
                descriptor = newDescriptor().accepts(targetClass.getDescriptor(), MH_ARRAY.getDescriptor()).toString();
            } else if(useInstance) {
                descriptor = newDescriptor().accepts(targetClass.getDescriptor()).toString();
            } else {
                descriptor = newDescriptor().accepts(MH_ARRAY.getDescriptor()).toString();
            }

            /* Generate constructor */
            mv = cv.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
            adapter = new GeneratorAdapter(mv, ACC_PUBLIC, "<init>", descriptor);
            mv.visitCode();
            adapter.loadThis();
            adapter.visitMethodInsn(INVOKESPECIAL, OBJECT.getInternalName(), "<init>", "()V", false);

            /* Start putting arguments to fields */
            if(useInstance && useMH) {
                adapter.loadThis();
                adapter.loadArg(0);
                adapter.putField(proxyClass, REFF, targetClass);
                adapter.loadThis();
                adapter.loadArg(1);
                adapter.putField(proxyClass, MHF, MH_ARRAY);
            } else if(useInstance) {
                adapter.loadThis();
                adapter.loadArg(0);
                adapter.putField(proxyClass, "ref", targetClass);
            } else /*if(useMH)*/ {
                adapter.loadThis();
                adapter.loadArg(0);
                adapter.putField(proxyClass, "mh", MH_ARRAY);
            }

            /* End constructor */
            adapter.returnValue();
            adapter.endMethod();

            /* Generate required fields as well */
            if(useMH) {
                fv = cv.visitField(ACC_PRIVATE | ACC_FINAL, "mh",
                        MH_ARRAY.getDescriptor(), null, null);
                fv.visitEnd();
            }
            if(useInstance) {
                fv = cv.visitField(ACC_PRIVATE | ACC_FINAL, "ref",
                        targetClass.getDescriptor(), null, null);
                fv.visitEnd();
            }
        } else {
            ClassTools.generateSimpleSuperConstructor(cv, Object.class);
        }
    }

    /* Generates proxy method, what invokes target method */
    static void generateMethodProxy(ClassVisitor cv, Method interfaceMethod,
                                    Type proxyClass, Type targetClass, Type interfaceClass,
                                    String targetMethodName, Type[] targetParameters, Type targetReturnType,
                                    boolean isTargetPublic, boolean useInstance,
                                    boolean useInterface, boolean useMH, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        mv.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, proxyClass, useMH, mhIndex);

        /* Load instance, if required */
        loadInstance(adapter, proxyClass, targetClass, useInstance, isTargetPublic);

        /* Load method parameters into stack */
        loadArguments(adapter, Type.getArgumentTypes(interfaceMethod), targetParameters);

        if(useMH) {
            /* Build MethodHandle descriptor (invokeExact is polymorphic) */
            String mhDescriptor = convertDesc(targetParameters, targetReturnType, useInstance ? targetClass : null);

            /* Invoke MethodHandle */
            mv.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), "invokeExact", mhDescriptor, false);
        } else {
            /* Figure out what opcode to use */
            int opCode = useInterface ?
                    (useInstance ? INVOKEINTERFACE : INVOKESTATIC)
                    :
                    (useInstance ? INVOKEVIRTUAL : INVOKESTATIC);

            /* Build descriptor & select target class */
            String targetDescriptor = convertDesc(targetParameters, targetReturnType, null);
            String targetName = useInterface ? interfaceClass.getInternalName() : targetClass.getInternalName();

            /* Invoke method */
            adapter.visitMethodInsn(opCode, targetName, targetMethodName, targetDescriptor, useInterface);
        }

        /* Return */
        handleReturn(adapter, interfaceMethod, targetReturnType);
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what invokes target's constructor */
    static void generateConstructorProxy(ClassVisitor cv, Method interfaceMethod,
                                         Type proxyClass, Type targetClass,
                                         Type[] targetParameters,
                                         boolean isTargetPublic, boolean useMH, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        String targetClassName = targetClass.getInternalName();
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        mv.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, proxyClass, useMH, mhIndex);

        if(!useMH) {
            adapter.visitTypeInsn(NEW, targetClassName);
            adapter.visitInsn(DUP);
        }

        /* Load method parameters into stack */
        loadArguments(adapter, Type.getArgumentTypes(interfaceMethod), targetParameters);

        if(useMH) {
            /* Build MethodHandle descriptor */
            String mhDescriptor = convertDesc(targetParameters,
                    isTargetPublic ? targetClass : OBJECT, null);

            /* Select right MethodHandle invoker */
            String mhInvoker = isTargetPublic? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            mv.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            /* Build target descriptor */
            String targetDesc = convertDesc(targetParameters, Type.VOID_TYPE, null);

            /* Invoke constructor */
            adapter.visitMethodInsn(INVOKESPECIAL, targetClassName, "<init>", targetDesc, false);
        }

        /* Return */
        handleReturn(adapter, interfaceMethod, isTargetPublic ? targetClass : OBJECT);
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what reads field */
    static void generateFieldReadMethod(ClassVisitor cv, Method interfaceMethod,
                                        Type proxyClass, Type targetClass, Type fieldType, String fieldName,
                                        boolean isTargetPublic, boolean useInstance,
                                        boolean useMH, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        mv.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, proxyClass, useMH, mhIndex);

        /* Load instance, if required */
        loadInstance(adapter, proxyClass, targetClass, useInstance, isTargetPublic);

        if(useMH) {
            /* Build MethodHandle descriptor */
            String mhDescriptor = newDescriptor()
                    .accepts(useInstance ? (isTargetPublic ? targetClass : OBJECT).getDescriptor() : "")
                    .returns(fieldType.getDescriptor())
                    .toString();

            /* Select right MethodHandle invoker */
            String mhInvoker = isTargetPublic? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            adapter.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            if(useInstance) {
                adapter.getField(targetClass, fieldName, fieldType);
            } else {
                adapter.getStatic(targetClass, fieldName, fieldType);
            }
        }

        /* Return */
        handleReturn(adapter, interfaceMethod, fieldType);
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what writes field */
    static void generateFieldWriteMethod(ClassVisitor cv, Method interfaceMethod,
                                        Type proxyClass, Type targetClass, Type fieldType, String fieldName,
                                         boolean isTargetPublic, boolean useInstance,
                                         boolean useMH, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        mv.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, proxyClass, useMH, mhIndex);

        /* Load instance, if required */
        loadInstance(adapter, proxyClass, targetClass, useInstance, isTargetPublic);

        /* Load method parameter into stack */
        adapter.loadArg(0);

        if(useMH) {
            /* Build MethodHandle descriptor */
            String mhDescriptor = newDescriptor()
                    .accepts((useInstance ?
                            (isTargetPublic ? targetClass : OBJECT).getDescriptor(): "")
                            + fieldType.getDescriptor())
                    .toString();

            /* Select right MethodHandle invoker */
            String mhInvoker = isTargetPublic? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            adapter.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            if(useInstance) {
                adapter.putField(targetClass, fieldName, fieldType);
            } else {
                adapter.putStatic(targetClass, fieldName, fieldType);
            }
        }

        /* Return */
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what just throws RuntimeException */
    static void generateFailedMethod(ClassVisitor cv, Method interfaceMethod, String errorMessage) {
        String methodName = interfaceMethod.getName();
        String methodDescriptor = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDescriptor);
        mv.visitCode();

        /* Throw exception */
        adapter.throwException(Type.getType(RuntimeException.class), errorMessage);

        /* End method generation */
        adapter.endMethod();
    }

    /* Helps to box/unbox parameters */
    private static void loadArguments(GeneratorAdapter ga, Type[] interfaceTypes, Type[] targetTypes) {
        Ensure.ensureCondition(interfaceTypes.length == targetTypes.length,
                "Interface and target parameter count don't match!");
        /* Iterate through all types */
        for(int i = 0; i < interfaceTypes.length; i++) {
            Type interfaceType = interfaceTypes[i];
            Type targetType = targetTypes[i];

            ga.loadArg(i);
            if(isPrimitive(interfaceType)) {
                if(!isPrimitive(targetType)) {
                    ga.box(targetType);
                }
            } else {
                if(isPrimitive(targetType)) {
                    ga.unbox(targetType);
                } else {
                    if(interfaceType.equals(OBJECT)) {
                        ga.checkCast(targetType);
                        ga.cast(interfaceType, targetType);
                    }
                }
            }
        }
    }

    /* Loads MethodHandle from array */
    private static void loadMH(GeneratorAdapter adapter, Type proxyClass, boolean useMH, int mhIndex) {
        if(!useMH) return;

        /* Load MethodHandle field */
        adapter.loadThis();
        adapter.getField(proxyClass, MHF, MH_ARRAY);

        /* Load index */
        adapter.visitIntInsn(BIPUSH, mhIndex);

        /* Load MethodHandle from array */
        adapter.visitInsn(AALOAD);
    }

    /* Loads class instance */
    private static void loadInstance(GeneratorAdapter adapter, Type proxyClass, Type targetClass,
                                     boolean useInstance, boolean isTargetPublic) {
        if(!useInstance) return;

        adapter.loadThis();
        adapter.getField(proxyClass, REFF, isTargetPublic ? targetClass : OBJECT);
    }

    /* Helps to convert Type[] and Type to descriptor String */
    @NotNull
    private static String convertDesc(@NotNull Type[] targetParameters, @NotNull Type returnType, @Nullable Type instanceType) {
        List<String> params = Stream.of(targetParameters).map(Type::getDescriptor).collect(Collectors.toList());
        if(instanceType != null) params.add(0, instanceType.getDescriptor());
        return newDescriptor()
                .accepts(params.toArray(new String[params.size()]))
                .returns(returnType.getDescriptor())
                .toString();
    }

    /* Handles return type boxing & casting */
    private static void handleReturn(GeneratorAdapter ga, Method interfaceMethod, Type targetReturnType) {
        Type returnType = Type.getReturnType(interfaceMethod);

        if(isPrimitive(returnType)) {
            if(!isPrimitive(targetReturnType)) {
                ga.unbox(targetReturnType);
            }
        } else {
            if(isPrimitive(targetReturnType)) {
                ga.box(targetReturnType);
            }
        }
    }

    /* Checks if type is primitive */
    private static boolean isPrimitive(Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return true;
            default:
                return false;
        }
    }
}
