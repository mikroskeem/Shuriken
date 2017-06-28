package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.instrumentation.ClassTools;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static eu.mikroskeem.shuriken.instrumentation.ClassTools.unqualifyName;
import static eu.mikroskeem.shuriken.instrumentation.Descriptor.newDescriptor;
import static org.objectweb.asm.Opcodes.*;


/**
 * @author Mark Vainomaa
 */
final class MethodGenerator {
    final static String MH = "java/lang/invoke/MethodHandle";
    final static String MH_ARRAY = "[L" + MH + ";";

    /* Generates appropriate constructor for class */
    static void generateConstructor(ClassVisitor classVisitor, boolean useInstance, boolean useMH,
                                    String proxyClassInternal, String targetClassInternal) {
        if(useInstance || useMH) {
            /* Figure out what descriptor to use */
            String descriptor;
            if(useInstance && useMH) {
                descriptor = newDescriptor().accepts("L" + targetClassInternal + ";", MH_ARRAY).toString();
            } else if(useInstance) {
                descriptor = newDescriptor().accepts("L" + targetClassInternal + ";").toString();
            } else {
                descriptor = newDescriptor().accepts(MH_ARRAY).toString();
            }

            MethodVisitor mv = classVisitor.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, unqualifyName(Object.class), "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);

            if(useInstance && useMH) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternal, "mh", MH_ARRAY);
            } else if(useInstance) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternal, "mh", MH_ARRAY);
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            ClassTools.generateSimpleSuperConstructor((ClassVisitor)classVisitor, Object.class);
        }
    }

    /* Generates proxy method for public target method */
    static void generateProxyMethod(ClassVisitor visitor, Method method, Method targetMethod,
                                    String proxyClassInternal, String targetClassInternal,
                                    boolean useInstance, boolean useInterface) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();
        String targetDescriptor = newDescriptor()
                .accepts(targetMethod.getParameterTypes())
                .returns(targetMethod.getReturnType())
                .toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load class reference into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L"+targetClassInternal+";");
        }

        /* Load all parameters into stack */
        loadParams(mv, method, targetMethod.getParameterTypes());

        /* Figure out what opcode to use */
        int opCode = useInterface?
                (useInstance ? INVOKEINTERFACE : INVOKESTATIC)
                :
                (useInstance ? INVOKEVIRTUAL : INVOKESTATIC);

        /* Target class is interface, if we can use interface */
        if(useInterface) targetClassInternal = ClassTools.unqualifyName(targetMethod.getDeclaringClass());

        /* Invoke target method */
        mv.visitMethodInsn(opCode, targetClassInternal, targetMethod.getName(), targetDescriptor, useInterface);

        /* Use appropriate return instruction */
        generateReturn(mv, method, targetMethod.getReturnType());

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates method for non-public target method */
    static void generateProxyMHMethod(ClassVisitor visitor, Method method, Method targetMethod,
                                      String proxyClassInternal, String targetClassInternal,
                                      boolean useInstance, int methodHandleIndex) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();

        /* Build MethodHandle accepts */
        StringBuilder mhAccepts = new StringBuilder();
        Arrays.stream(targetMethod.getParameterTypes()).map(Type::getDescriptor).forEach(mhAccepts::append);
        String mhDescriptor = newDescriptor()
                .accepts((useInstance? "L"+targetClassInternal+";" : "") + mhAccepts.toString())
                .returns(targetMethod.getReturnType())
                .toString();

        /* Define method */
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load MethodHandle from array */
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyClassInternal, "mh", MH_ARRAY);
        mv.visitIntInsn(BIPUSH, methodHandleIndex);
        mv.visitInsn(AALOAD);

        /* Load class reference into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
        }

        /* Load params */
        loadParams(mv, method, targetMethod.getParameterTypes());

        /* Invoke MethodHandle */
        mv.visitMethodInsn(INVOKEVIRTUAL, MH, "invokeExact", mhDescriptor, false);

        /* Use appropriate return instruction */
        generateReturn(mv, method, targetMethod.getReturnType());

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates field reader method */
    static void generateFieldReadMethod(ClassVisitor visitor, Method method, Field field,
                                        String proxyClassInternal, String targetClassInternal,
                                        boolean useInstance) {
        String descriptor = newDescriptor().returns(method.getReturnType()).toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load class instance into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
        }

        /* Read field */
        mv.visitFieldInsn(useInstance? GETFIELD : GETSTATIC, targetClassInternal, field.getName(), Type.getDescriptor(field.getType()));

        /* Use appropriate return instruction */
        generateReturn(mv, method, field.getType());

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates field reader method (using MethodHandle) */
    static void generateFieldReadMHMethod(ClassVisitor visitor, Method method, Field field,
                                          String proxyClassInternal, String targetClassInternal,
                                          boolean useInstance, int methodHandleIndex) {
        String descriptor = newDescriptor().returns(method.getReturnType()).toString();
        String mhDescriptor = newDescriptor()
                .accepts((useInstance? "L"+targetClassInternal+";" : ""))
                .returns(field.getType())
                .toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load MethodHandle from array */
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyClassInternal, "mh", MH_ARRAY);
        mv.visitIntInsn(BIPUSH, methodHandleIndex);
        mv.visitInsn(AALOAD);

        /* Load class reference into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
        }

        /* Invoke MethodHandle */
        mv.visitMethodInsn(INVOKEVIRTUAL, MH, "invokeExact", mhDescriptor, false);

        /* Use appropriate return instruction */
        generateReturn(mv, method, field.getType());

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates field writer method */
    static void generateFieldWriteMethod(ClassVisitor visitor, Method method, Field field,
                                         String proxyClassInternal, String targetClassInternal,
                                         boolean useInstance) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()[0]).toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load class reference into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
        }

        /* Load parameters into stack */
        loadParams(mv, method, field.getType());

        /* Put value into field */
        mv.visitFieldInsn(useInstance? PUTFIELD : PUTSTATIC, targetClassInternal, field.getName(), Type.getDescriptor(field.getType()));

        /* Setters always return void */
        mv.visitInsn(RETURN);

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates field writer method (using MethodHandle) */
    static void generateFieldWriteMHMethod(ClassVisitor visitor, Method method, Field field,
                                           String proxyClassInternal, String targetClassInternal,
                                           boolean useInstance, int methodHandleIndex) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()[0]).toString();
        String mhDescriptor = newDescriptor()
                .accepts((useInstance? "L"+targetClassInternal+";" : "") + Type.getDescriptor(field.getType()))
                .toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load MethodHandle from array */
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyClassInternal, "mh", MH_ARRAY);
        mv.visitIntInsn(BIPUSH, methodHandleIndex);
        mv.visitInsn(AALOAD);

        /* Load class reference into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L" + targetClassInternal + ";");
        }

        /* Load parameters into stack */
        loadParams(mv, method, field.getType());

        /* Invoke MethodHandle */
        mv.visitMethodInsn(INVOKEVIRTUAL, MH, "invokeExact", mhDescriptor, false);

        /* Setters always return void */
        mv.visitInsn(RETURN);

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates method for public constructor target */
    static void generateConstructorMethod(ClassVisitor visitor, Method method, Constructor<?> constructor,
                                          String proxyClassInternal, String targetClassInternal) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();
        String ctorDesc = newDescriptor().accepts(constructor.getParameterTypes()).toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Invoke constructor */
        mv.visitTypeInsn(NEW, targetClassInternal);
        mv.visitInsn(DUP);

        /* Load parameters */
        loadParams(mv, method, constructor.getParameterTypes());

        /* Call constructor `<init>` */
        mv.visitMethodInsn(INVOKESPECIAL, targetClassInternal, "<init>", ctorDesc, false);

        /* Use appropriate return instruction */
        generateReturn(mv, method, constructor.getDeclaringClass());

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates method for non-public constructor target (using MethodHandle) */
    static void generateConstructorMHMethod(ClassVisitor visitor, Method method, Constructor<?> constructor,
                                            String proxyClassInternal, String targetClassInternal,
                                            int methodHandleIndex) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();
        String mhDescriptor = newDescriptor().accepts(constructor.getParameterTypes())
                .returns(constructor.getDeclaringClass()).toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load MethodHandle from array */
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyClassInternal, "mh", MH_ARRAY);
        mv.visitIntInsn(BIPUSH, methodHandleIndex);
        mv.visitInsn(AALOAD);

        /* Load parameters into stack */
        loadParams(mv, method, constructor.getParameterTypes());

        /* Invoke MethodHandle */
        mv.visitMethodInsn(INVOKEVIRTUAL, MH, "invokeExact", mhDescriptor, false);

        /* Use appropriate return instruction */
        generateReturn(mv, method, constructor.getDeclaringClass());

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates exception throwing method */
    static void generateFailedMethod(ClassVisitor visitor, Method method, String exceptionMessage) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Generate exception throw */
        String RE = ClassTools.unqualifyName(RuntimeException.class);
        String RE_DESC = newDescriptor().accepts(String.class).toString();
        mv.visitTypeInsn(NEW, RE);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(exceptionMessage);
        mv.visitMethodInsn(INVOKESPECIAL, RE, "<init>", RE_DESC, false);
        mv.visitInsn(ATHROW);

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Loads parameters into stack with correct instructions */
    static void loadParams(MethodVisitor methodVisitor, Method method, Class<?>... targetTypes) {
        /* TODO: Handle target method return type cases as well */
        if(method.getParameterCount() > 0) {
            for (int i = 0; i < method.getParameterCount(); i++) {
                if (method.getParameterTypes()[i].isPrimitive()) {
                    switch (method.getParameterTypes()[i].getName()) {
                        case "double":
                            methodVisitor.visitVarInsn(DLOAD, i + 1);
                            break;
                        case "float":
                            methodVisitor.visitVarInsn(FLOAD, i + 1);
                            break;
                        case "long":
                            methodVisitor.visitVarInsn(LLOAD, i + 1);
                            break;
                        case "boolean":
                        case "byte":
                        case "short":
                        case "char":
                        case "int":
                            methodVisitor.visitVarInsn(ILOAD, i + 1);
                            break;
                        case "void":
                            break;
                    }
                } else {
                    methodVisitor.visitVarInsn(ALOAD, i + 1);
                }
            }
        }
    }

    /* Inserts correct return instruction */
    static void generateReturn(MethodVisitor methodVisitor, Method method, Class<?> returnType) {
        /* TODO: Handle return type cases as well */
        if(method.getReturnType().isPrimitive()) {
            switch (method.getReturnType().getName()) {
                case "double":
                    methodVisitor.visitInsn(DRETURN);
                    break;
                case "float":
                    methodVisitor.visitInsn(FRETURN);
                    break;
                case "long":
                    methodVisitor.visitInsn(LRETURN);
                    break;
                case "boolean":
                case "byte":
                case "short":
                case "char":
                case "int":
                    methodVisitor.visitInsn(IRETURN);
                    break;
                case "void":
                    methodVisitor.visitInsn(RETURN);
                    break;
            }
        } else {
            methodVisitor.visitInsn(method.getReturnType() == Void.class ? RETURN : ARETURN);
        }
    }
}
