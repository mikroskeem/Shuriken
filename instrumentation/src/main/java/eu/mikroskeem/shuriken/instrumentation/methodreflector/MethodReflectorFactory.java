package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.instrumentation.ClassTools;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.mikroskeem.shuriken.instrumentation.ClassTools.unqualifyName;
import static eu.mikroskeem.shuriken.instrumentation.Descriptor.newDescriptor;
import static org.objectweb.asm.Opcodes.*;


/**
 * @author Mark Vainomaa
 */
final class MethodReflectorFactory {
    private static final Map<ClassWrapper<?>, AtomicInteger> COUNTER = new HashMap<>();
    private final GeneratedClassLoader GCL = new GeneratedClassLoader(MethodReflectorFactory.class.getClassLoader());

    /* Other constants */
    private final String MH = "java/lang/invoke/MethodHandle";
    private final String MH_ARRAY = "[L" + MH + ";";

    @SuppressWarnings("unchecked")
    <T> T generateReflector(ClassWrapper<?> target, Class<T> intf) {
        Method[] targetDeclaredMethods = target.getWrappedClass().getDeclaredMethods();
        boolean mustUseInstance = false;

        /* MethodHandle-related */
        List<MethodHandle> methodHandles = new ArrayList<>();
        MethodHandles.Lookup mhLookup = MethodHandles.lookup();
        boolean mustUseMH = false;

        /* ** Do safety checks first */

        /*
         * Check if all methods are present,
         * also check if interface is targeting static methods, so we can omit using class instance
         */
        for (Method method : intf.getMethods()) {
            try {
                /* Find method */
                Method targetMethod = Arrays.stream(targetDeclaredMethods)
                        .filter(m -> Arrays.equals(method.getParameterTypes(), m.getParameterTypes()) &&
                                method.getReturnType() == m.getReturnType()).findFirst().orElse(null);
                if(targetMethod == null) throw new NoSuchMethodException(method.getName());

                /* Check for static annotation */
                if(!Modifier.isStatic(targetMethod.getModifiers())) {
                    if(!mustUseInstance) mustUseInstance = true;
                }

                /* Check for non-public methods */
                if(!Modifier.isPublic(targetMethod.getModifiers())) {
                    if(!mustUseMH) mustUseMH = true;
                }
            } catch (NoSuchMethodException e) {
                System.out.println("WARN: Skipping method because of NSME: " + e);
            }
        }

        /* Check if we have class instance in ClassWrapper */
        if(mustUseInstance) {
            Ensure.ensureCondition(target.getClassInstance() != null, "Interface targets instance methods, " +
                    "but class instance is not present in ClassWrapper!");
        }

        String proxyClassName = generateName(target, intf);
        String proxyClassInternalName = unqualifyName(proxyClassName);
        Class<?> targetClass = target.getWrappedClass();
        String targetClassInternalName = unqualifyName(targetClass);

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        //ClassVisitor classWriter = new TraceClassVisitor(classWriter, new PrintWriter(System.out));

        classWriter.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
                proxyClassInternalName,
                null,
                unqualifyName(Object.class),
                new String[]{unqualifyName(intf)}
        );

        /* Set up needed field for class instance reference */
        if(mustUseInstance) {
            FieldVisitor fv = classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "ref", "L" + targetClassInternalName + ";", null, null);
            fv.visitEnd();
        }

        /* Set up needed field for MethodHandle array */
        if(mustUseMH) {
            FieldVisitor fv = classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "mh", MH_ARRAY, null, null);
            fv.visitEnd();
        }


        /* Generate constructor */
        if(mustUseInstance || mustUseMH) {
            /* Figure out what descriptor to use */
            String descriptor;
            if(mustUseInstance && mustUseMH) {
                descriptor = newDescriptor().accepts(Type.getDescriptor(target.getWrappedClass()), MH_ARRAY).toString();
            } else if(mustUseInstance) {
                descriptor = newDescriptor().accepts(target.getWrappedClass()).toString();
            } else {
                descriptor = newDescriptor().accepts(MH_ARRAY).toString();
            }

            MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, unqualifyName(Object.class), "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);

            if(mustUseInstance && mustUseMH) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternalName, "ref", "L" + targetClassInternalName + ";");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternalName, "mh", MH_ARRAY);
            } else if(mustUseInstance) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternalName, "ref", "L" + targetClassInternalName + ";");
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, proxyClassInternalName, "mh", MH_ARRAY);
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            ClassTools.generateSimpleSuperConstructor((ClassVisitor)classWriter, Object.class);
        }


        /* Generate proxy methods */
        for (Method method : intf.getMethods()) {
            try {
                /* Try to get target method */
                Method targetMethod = targetClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                if(targetMethod.getReturnType() != method.getReturnType()) continue;

                /* Get needed info */
                boolean isPublic = Modifier.isPublic(targetMethod.getModifiers());
                boolean isStatic = Modifier.isStatic(targetMethod.getModifiers());

                /* Generate proxy method */
                if(isPublic) {
                    generateMethod(classWriter, method, proxyClassInternalName, targetClassInternalName, !isStatic);
                } else {
                    /* Add MethodHandle into MethodHandles array */
                    if(!targetMethod.isAccessible()) targetMethod.setAccessible(true);
                    methodHandles.add(mhLookup.unreflect(targetMethod));

                    generateMHMethod(classWriter, method,
                            proxyClassInternalName, targetClassInternalName,
                            !isStatic, methodHandles.size() - 1);
                }
            }
            catch (NoSuchMethodException|IllegalAccessException ignored) {}
        }
        classWriter.visitEnd();

        /* Load accessor */
        byte[] classData = classWriter.toByteArray();
        ClassWrapper<?> accessor = Reflect.wrapClass(GCL.defineClass(proxyClassName, classData));

        /* Construct accessor */
        List<TypeWrapper> ctorParams = new ArrayList<>();
        if(mustUseInstance) ctorParams.add(TypeWrapper.of(targetClass, target.getClassInstance()));
        if(mustUseMH) ctorParams.add(TypeWrapper.of(MethodHandle[].class,
                methodHandles.toArray(new MethodHandle[methodHandles.size()])));

        return (T) accessor.construct(ctorParams.toArray(new TypeWrapper[ctorParams.size()])).getClassInstance();
    }

    @NotNull
    private String generateName(ClassWrapper<?> target, Class<?> intf) {
        StringBuilder classNameBuilder = new StringBuilder();
        classNameBuilder.append(MethodReflector.class.getPackage().getName());
        classNameBuilder.append(".");
        classNameBuilder.append("Target$");
        classNameBuilder.append(getClassName(target.getWrappedClass().getName()));
        classNameBuilder.append("$");
        classNameBuilder.append(getClassName(intf.getName()));
        classNameBuilder.append("$");
        classNameBuilder.append(COUNTER.computeIfAbsent(target, k -> new AtomicInteger(0)).getAndIncrement());
        return classNameBuilder.toString();
    }

    /* Generates proxy method for public target method */
    private void generateMethod(ClassVisitor visitor, Method method,
                                String proxyClassInternal, String targetClassInternal,
                                boolean useInstance) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();
        MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, method.getName(), descriptor, null, null);
        mv.visitCode();

        /* Load class reference into stack, if needed */
        if(useInstance) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, proxyClassInternal, "ref", "L"+targetClassInternal+";");
        }

        /* Load all parameters into stack */
        loadParams(mv, method);

        /* Invoke target method */
        mv.visitMethodInsn(useInstance? INVOKEVIRTUAL : INVOKESTATIC, targetClassInternal, method.getName(), descriptor, false);

        /* Use appropriate return instruction */
        generateReturn(mv, method);

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Generates method for non-public target method */
    private void generateMHMethod(ClassVisitor visitor, Method method,
                                  String proxyClassInternal, String targetClassInternal,
                                  boolean useInstance, int methodHandleIndex) {
        String descriptor = newDescriptor().accepts(method.getParameterTypes()).returns(method.getReturnType()).toString();

        /* Build MethodHandle accepts */
        StringBuilder mhAccepts = new StringBuilder();
        Arrays.stream(method.getParameterTypes()).map(Type::getDescriptor).forEach(mhAccepts::append);
        String mhDescriptor = newDescriptor()
                .accepts((useInstance? "L"+targetClassInternal+";" : "") + mhAccepts.toString())
                .returns(method.getReturnType())
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
        loadParams(mv, method);

        /* Invoke MethodHandle */
        mv.visitMethodInsn(INVOKEVIRTUAL, MH, "invokeExact", mhDescriptor, false);

        /* Use appropriate return instruction */
        generateReturn(mv, method);

        /* End of the method */
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /* Loads parameters into stack with correct instructions */
    private void loadParams(MethodVisitor methodVisitor, Method method) {
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
    private void generateReturn(MethodVisitor methodVisitor, Method method) {
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

    @NotNull
    private String getClassName(String name) {
        return name.substring(name.lastIndexOf('.') + 1, name.length());
    }
}