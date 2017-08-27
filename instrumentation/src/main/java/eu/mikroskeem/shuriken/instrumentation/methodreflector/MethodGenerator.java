package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.instrumentation.ClassTools;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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

import static eu.mikroskeem.shuriken.common.Ensure.notNull;
import static eu.mikroskeem.shuriken.instrumentation.Descriptor.newDescriptor;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;


/**
 * Proxy class method generator
 *
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
    @Contract("null, _, null, null -> fail")
    static void generateClassBase(final ClassVisitor cv, final int flags, final Type reflectorClass, Type targetClass) {
        FieldVisitor fv = null;
        GeneratorAdapter adapter;
        MethodVisitor mv;

        if((flags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0 || (flags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) {
            /* Use java.lang.Object instead */
            if((flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) == 0)
                targetClass = OBJECT;

            /* Figure out what descriptor to use */
            String descriptor = null;
            if((flags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0 && (flags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) {
                descriptor = newDescriptor().accepts(targetClass.getDescriptor(), MH_ARRAY.getDescriptor()).toString();
            } else if((flags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) {
                descriptor = newDescriptor().accepts(targetClass.getDescriptor()).toString();
            } else if((flags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0) {
                descriptor = newDescriptor().accepts(MH_ARRAY.getDescriptor()).toString();
            } else {
                descriptor.getClass(); // Explicit NPE
            }

            /* Generate constructor */
            mv = cv.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
            adapter = new GeneratorAdapter(mv, ACC_PUBLIC, "<init>", descriptor);
            adapter.visitCode();
            adapter.loadThis();
            adapter.visitMethodInsn(INVOKESPECIAL, OBJECT.getInternalName(), "<init>", "()V", false);

            /* Start putting arguments to fields */
            if((flags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0 && (flags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) {
                adapter.loadThis();
                adapter.loadArg(0);
                adapter.putField(reflectorClass, REFF, targetClass);
                adapter.loadThis();
                adapter.loadArg(1);
                adapter.putField(reflectorClass, MHF, MH_ARRAY);
            } else if((flags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) {
                adapter.loadThis();
                adapter.loadArg(0);
                adapter.putField(reflectorClass, REFF, targetClass);
            } else /*if((flags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0)*/ {
                adapter.loadThis();
                adapter.loadArg(0);
                adapter.putField(reflectorClass, MHF, MH_ARRAY);
            }

            /* End constructor */
            adapter.returnValue();
            adapter.endMethod();

            /* Generate required fields as well */
            if((flags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0) fv = cv.visitField(ACC_PRIVATE | ACC_FINAL, "mh",
                        MH_ARRAY.getDescriptor(), null, null);

            if((flags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) fv = cv.visitField(ACC_PRIVATE | ACC_FINAL, "ref",
                        targetClass.getDescriptor(), null, null);

            notNull(fv, "FieldVisitor shouldn't be null!");
            fv.visitEnd();
        } else {
            ClassTools.generateSimpleSuperConstructor(cv, Object.class);
        }
    }

    /* Generates proxy method, what invokes target method */
    @Contract("null, null, null, null, null, null, null, null, _, _ -> fail")
    static void generateMethodProxy(ClassVisitor cv, Method interfaceMethod,
                                    Type reflectorClass, Type targetClass, Type interfaceClass,
                                    String targetMethodName, Type[] targetParameters, Type targetReturnType,
                                    int flags, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        adapter.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, reflectorClass, flags, mhIndex);

        /* Load instance, if required */
        loadInstance(adapter, reflectorClass, targetClass, flags);

        /* Load method parameters into stack */
        loadArguments(adapter, Type.getArgumentTypes(interfaceMethod), targetParameters, (flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0);

        if((flags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
            /* Build MethodHandle descriptor (invokeExact is polymorphic) */
            String mhDescriptor = convertDesc(targetParameters,
                    ((flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? targetReturnType : OBJECT),
                    (flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0 ? ((flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 ? targetClass : OBJECT) : null);

            /* Select right MethodHandle invoker */
            String mhInvoker = (flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 && (flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            mv.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            /* Figure out what opcode to use */
            int opCode = (flags & Magic.REFLECTOR_METHOD_USE_INVOKEINTERFACE) != 0 ?
                    ((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0 ? INVOKEINTERFACE : INVOKESTATIC)
                    :
                    ((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0 ? INVOKEVIRTUAL : INVOKESTATIC);

            /* Build descriptor & select target class */
            String targetDescriptor = convertDesc(targetParameters, targetReturnType, null);
            String targetName = (flags & Magic.REFLECTOR_METHOD_USE_INVOKEINTERFACE) != 0 ? interfaceClass.getInternalName() : targetClass.getInternalName();

            /* Invoke method */
            adapter.visitMethodInsn(opCode, targetName, targetMethodName, targetDescriptor, (flags & Magic.REFLECTOR_METHOD_USE_INVOKEINTERFACE) != 0);
        }

        /* Return */
        handleReturn(adapter, interfaceMethod, targetReturnType);
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what invokes target's constructor */
    @Contract("null, null, null, null, null, _, _ -> fail")
    static void generateConstructorProxy(ClassVisitor cv, Method interfaceMethod,
                                         Type reflectorClass, Type targetClass,
                                         Type[] targetParameters,
                                         int flags, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        String targetClassName = targetClass.getInternalName();
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        adapter.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, reflectorClass, flags, mhIndex);

        if((flags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) == 0) {
            adapter.visitTypeInsn(NEW, targetClassName);
            adapter.visitInsn(DUP);
        }

        /* Load method parameters into stack */
        loadArguments(adapter, Type.getArgumentTypes(interfaceMethod), targetParameters, (flags & Magic.RETURN_TYPE_PUBLIC) != 0);

        if((flags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
            /* Build MethodHandle descriptor */
            String mhDescriptor = convertDesc(targetParameters,
                    (flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? targetClass : OBJECT, null);

            /* Select right MethodHandle invoker */
            String mhInvoker = (flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            mv.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            /* Build target descriptor */
            String targetDesc = convertDesc(targetParameters, Type.VOID_TYPE, null);

            /* Invoke constructor */
            adapter.visitMethodInsn(INVOKESPECIAL, targetClassName, "<init>", targetDesc, false);
        }

        /* Return */
        handleReturn(adapter, interfaceMethod, (flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? targetClass : OBJECT);
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what reads field */
    @Contract("null, null, null, null, null, null, _, _ -> fail")
    static void generateFieldReadMethod(ClassVisitor cv, Method interfaceMethod,
                                        Type reflectorClass, Type targetClass, Type fieldType, String fieldName,
                                        int flags, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        adapter.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, reflectorClass, flags, mhIndex);

        /* Load instance, if required */
        loadInstance(adapter, reflectorClass, targetClass, flags);

        if((flags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
            /* Build MethodHandle descriptor */
            String mhDescriptor = newDescriptor()
                    .accepts((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0 ? ((flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 ? targetClass : OBJECT).getDescriptor() : "")
                    .returns(fieldType.getDescriptor())
                    .toString();

            /* Select right MethodHandle invoker */
            String mhInvoker = (flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 && (flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            adapter.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            if((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0)
                adapter.getField(targetClass, fieldName, fieldType);
            else
                adapter.getStatic(targetClass, fieldName, fieldType);
        }

        /* Return */
        handleReturn(adapter, interfaceMethod, fieldType);
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what writes field */
    @Contract("null, null, null, null, null, null, _, _ -> fail")
    static void generateFieldWriteMethod(ClassVisitor cv, Method interfaceMethod,
                                         Type reflectorClass, Type targetClass, Type fieldType, String fieldName,
                                         int flags, int mhIndex) {
        String methodName = interfaceMethod.getName();
        String methodDesc = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDesc);
        adapter.visitCode();

        /* Load MethodHandle, if required */
        loadMH(adapter, reflectorClass, flags, mhIndex);

        /* Load instance, if required */
        loadInstance(adapter, reflectorClass, targetClass, flags);

        /* Load method parameter into stack */
        adapter.loadArg(0);

        if((flags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
            /* Build MethodHandle descriptor */
            String mhDescriptor = newDescriptor()
                    .accepts(((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0 ?
                            ((flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 ? targetClass : OBJECT).getDescriptor(): "")
                            + fieldType.getDescriptor())
                    .toString();

            /* Select right MethodHandle invoker */
            String mhInvoker = (flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 && (flags & Magic.RETURN_TYPE_PUBLIC) != 0 ? "invokeExact" : "invoke";

            /* Invoke MethodHandle */
            adapter.visitMethodInsn(INVOKEVIRTUAL, MH.getInternalName(), mhInvoker, mhDescriptor, false);
        } else {
            if((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) != 0)
                adapter.putField(targetClass, fieldName, fieldType);
            else
                adapter.putStatic(targetClass, fieldName, fieldType);
        }

        /* Return */
        adapter.returnValue();

        /* End method */
        adapter.endMethod();
    }

    /* Generates method, what just throws RuntimeException */
    @Contract("null, null, null -> fail")
    static void generateFailedMethod(ClassVisitor cv, Method interfaceMethod, String errorMessage) {
        String methodName = interfaceMethod.getName();
        String methodDescriptor = Type.getMethodDescriptor(interfaceMethod);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, null, null);
        GeneratorAdapter adapter = new GeneratorAdapter(mv, ACC_PUBLIC, methodName, methodDescriptor);
        adapter.visitCode();

        /* Throw exception */
        adapter.throwException(Type.getType(RuntimeException.class), errorMessage);

        /* End method generation */
        adapter.endMethod();
    }

    /* Helps to box/unbox parameters */
    @Contract("null, null, null, !null -> fail")
    private static void loadArguments(GeneratorAdapter ga, Type[] interfaceTypes, Type[] targetTypes, boolean isTargetPublic) {
        Ensure.ensureCondition(interfaceTypes.length == targetTypes.length,
                "Interface and target parameter count don't match!");
        /* Iterate through all types */
        for(int i = 0; i < interfaceTypes.length; i++) {
            Type interfaceType = interfaceTypes[i];
            Type targetType = targetTypes[i];

            ga.loadArg(i);

            /* Do not do boxing/unboxing if MethodHandle.invoke is used, it handles them on its own */
            if(!isTargetPublic) continue;

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
    @Contract("null, null, _, _ -> fail")
    private static void loadMH(GeneratorAdapter adapter, Type reflectorClass, int flags, int mhIndex) {
        if((flags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) == 0) return;

        /* Load MethodHandle field */
        adapter.loadThis();
        adapter.getField(notNull(reflectorClass, "Reflector class shouldn't be null!"), MHF, MH_ARRAY);

        /* Load index */
        if(mhIndex >= 0 && mhIndex <= 5)
            /* ICONST_x offset is 3, iow ICONST_0 = 3, ICONST_1 = 4 */
            adapter.visitInsn(ICONST_0 + mhIndex);
        else
            adapter.visitIntInsn(BIPUSH, mhIndex);

        /* Load MethodHandle from array */
        adapter.visitInsn(AALOAD);
    }

    /* Loads class instance */
    @Contract("null, null, null, _ -> fail")
    private static void loadInstance(GeneratorAdapter adapter, Type reflectorClass, Type targetClass, int flags) {
        if((flags & Magic.REFLECTOR_METHOD_USE_INSTANCE) == 0) return;

        adapter.loadThis();
        adapter.getField(reflectorClass, REFF, (flags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0 ? targetClass : OBJECT);
    }

    /* Helps to convert Type[] and Type to descriptor String */
    @NotNull
    @Contract("null, null, _ -> fail")
    private static String convertDesc(Type[] targetParameters, Type returnType, Type instanceType) {
        List<String> params = Stream.of(targetParameters).map(Type::getDescriptor).collect(Collectors.toList());
        if(instanceType != null) params.add(0, instanceType.getDescriptor());
        return newDescriptor()
                .accepts(params.toArray(new String[params.size()]))
                .returns(returnType.getDescriptor())
                .toString();
    }

    /* Handles return type boxing & casting */
    @Contract("null, null, null -> fail")
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
    @Contract("null -> fail")
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
