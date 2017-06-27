package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.mikroskeem.shuriken.instrumentation.ClassTools.unqualifyName;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.*;
import static org.objectweb.asm.Opcodes.*;


/**
 * @author Mark Vainomaa
 */
final class MethodReflectorFactory {
    private static final Map<Class<?>, AtomicInteger> COUNTER = new WeakHashMap<>();
    private final GeneratedClassLoader GCL = new GeneratedClassLoader(MethodReflectorFactory.class.getClassLoader());

    /* Error messages */
    private final static String NO_CLASS_INSTANCE_PRESET = "Interface targets instance methods, but class instance " +
            "is not present in ClassWrapper!";
    private final static String ANNOTATION_ERROR = "Interface method can only have one target or field annotation! ";
    private final static String FIELD_NAME_IS_NULL = "Field name shouldn't be null!";
    private final static String FAILED_TO_UNREFLECT = "Failed to unreflect target: ";
    private final static String FAILED_TO_FIND_FIELD = "Could not find target field for interface method: ";
    private final static String FAILED_TO_FIND_METHOD = "Could not find target method for interface method: ";
    private final static String SETTER_WRONG_RETURN_TYPE = "Setters can only return void type! ";
    private final static String SETTER_WRONG_PARAM_COUNT = "Setters can only take one argument! ";
    private final static String GETTER_WRONG_RETURN_TYPE = "Getters can't return void type! ";
    private final static String GETTER_WRONG_PARAM_COUNT = "Getters can't take any arguments! ";
    private final static String CTOR_INVOKER_WRONG_RETURN_TYPE = "Constructor invoker's return type must match interface " +
            "method's return type! ";

    @SuppressWarnings("unchecked")
    <T> T generateReflector(ClassWrapper<?> target, Class<T> intf) {
        List<MethodHandle> methodHandles = new ArrayList<>();
        MethodHandles.Lookup mhLookup = MethodHandles.lookup();
        boolean classMustUseInstance = false;
        boolean classMustUseMH = false;

        String proxyClassName = generateName(target, intf);
        String proxyClassInternalName = unqualifyName(proxyClassName);
        Class<?> targetClass = target.getWrappedClass();
        String targetClassInternalName = unqualifyName(targetClass);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor classWriter = cw;
        //classWriter = new CheckClassAdapter(new TraceClassVisitor(classWriter, new PrintWriter(System.out)), false);

        classWriter.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
                proxyClassInternalName,
                null,
                unqualifyName(Object.class),
                new String[]{unqualifyName(intf)}
        );

        /* Generate proxy methods */
        for (Method intfMethod : intf.getMethods()) {
            /* Do not allow multiple annotations, because that ain't going to work anyway */
            boolean hasAnnotation = false;
            TargetFieldGetter fieldGetter = intfMethod.getAnnotation(TargetFieldGetter.class);
            TargetFieldSetter fieldSetter = intfMethod.getAnnotation(TargetFieldSetter.class);
            TargetMethod targetMethod = intfMethod.getAnnotation(TargetMethod.class);
            TargetConstructor targetConstructor = intfMethod.getAnnotation(TargetConstructor.class);
            if(fieldGetter != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + intfMethod); hasAnnotation = true; }
            if(fieldSetter != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + intfMethod); hasAnnotation = true; }
            if(targetMethod != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + intfMethod); hasAnnotation = true; }
            if(targetConstructor != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + intfMethod); hasAnnotation = true; }

            /* Create field getter/setter */
            if(fieldGetter != null || fieldSetter != null) {
                String fieldName = Ensure.notNull(either(fieldGetter, fieldSetter, "value"), FIELD_NAME_IS_NULL);
                boolean isSetter = fieldSetter != null;
                Class<?> fieldType;

                /*
                 * Force setters to return void and take only one parameter,
                 * force getters to return type and take no parameters
                 */
                if(isSetter) {
                   Ensure.ensureCondition(intfMethod.getReturnType() == void.class,
                           SETTER_WRONG_RETURN_TYPE + intfMethod);
                   Ensure.ensureCondition(intfMethod.getParameterTypes().length == 1,
                           SETTER_WRONG_PARAM_COUNT + intfMethod);
                   fieldType = intfMethod.getParameterTypes()[0];
                } else {
                    Ensure.ensureCondition(intfMethod.getReturnType() != void.class,
                            GETTER_WRONG_RETURN_TYPE + intfMethod);
                    Ensure.ensureCondition(intfMethod.getParameterTypes().length == 0,
                            GETTER_WRONG_PARAM_COUNT + intfMethod);
                    fieldType = intfMethod.getReturnType();
                }

                /* Try to find field */
                Field targetClassField = findDeclaredField(targetClass, fieldName, fieldType);

                /* Ensure target field is present */
                Ensure.notNull(targetClassField, FAILED_TO_FIND_FIELD + intfMethod);

                /* Get needed target field info */
                boolean useInstance = false;
                boolean useMH = false;

                if(!Modifier.isStatic(targetClassField.getModifiers())) {
                    /* Proxy class must use target class instance */
                    if(!classMustUseInstance) classMustUseInstance = true;
                    useInstance = true;
                }

                if(!Modifier.isPublic(targetClassField.getModifiers())) {
                    /* Proxy class must use method handles */
                    if(!classMustUseMH) classMustUseMH = true;
                    useMH = true;
                }

                if(Modifier.isFinal(targetClassField.getModifiers())) {
                    /* Proxy class must use method handles */
                    if(!classMustUseMH) classMustUseMH = true;
                    useMH = true;

                    /* Remove final modifier */
                    int modifiers = targetClassField.getModifiers();
                    Reflect.wrapInstance(targetClassField).getField("modifiers", int.class)
                            .ifPresent(fw -> fw.write(modifiers & ~Modifier.FINAL));
                }

                /* Generate method */
                if(useMH) {
                    MethodHandle methodHandle;

                    /* Try to look up method handle */
                    try {
                        if(!targetClassField.isAccessible()) targetClassField.setAccessible(true);
                        methodHandle = isSetter? mhLookup.unreflectSetter(targetClassField) :
                                mhLookup.unreflectGetter(targetClassField);
                    } catch (IllegalAccessException e) {
                        generateFailedMethod(classWriter, intfMethod, FAILED_TO_UNREFLECT + targetClassField);
                        continue;
                    }

                    /* Add MethodHandle into MethodHandles array */
                    methodHandles.add(methodHandle);

                    if(isSetter) {
                        generateFieldWriteMHMethod(classWriter, intfMethod, targetClassField,
                                proxyClassInternalName, targetClassInternalName,
                                useInstance, methodHandles.size() - 1);
                    } else {
                        generateFieldReadMHMethod(classWriter, intfMethod, targetClassField,
                                proxyClassInternalName, targetClassInternalName,
                                useInstance, methodHandles.size() - 1);
                    }
                } else {
                    if(isSetter) {
                        generateFieldWriteMethod(classWriter, intfMethod, targetClassField,
                                proxyClassInternalName, targetClassInternalName,
                                useInstance);
                    } else {
                        generateFieldReadMethod(classWriter, intfMethod, targetClassField,
                                proxyClassInternalName, targetClassInternalName,
                                useInstance);
                    }
                }

                /* Start loop from beginning, code below is meant for method proxy */
                continue;
            }

            /* Create constructor proxy */
            if(targetConstructor != null) {
                /* Force method to return target class type */
                Ensure.ensureCondition(intfMethod.getReturnType() == targetClass, CTOR_INVOKER_WRONG_RETURN_TYPE);

                Class<?>[] params = intfMethod.getParameterTypes();

                /* Try to find constructor */
                Constructor<?> classTargetConstructor = findDeclaredConstructor(targetClass, params);

                /* Get needed target constructor info */
                boolean useMH = false;

                if(!Modifier.isPublic(classTargetConstructor.getModifiers())) {
                    /* Proxy class must use method handles */
                    if(!classMustUseMH) classMustUseMH = true;
                    useMH = true;
                }

                /* Generate proxy method */
                if(useMH) {
                    MethodHandle methodHandle;
                    try {
                        if(!classTargetConstructor.isAccessible()) classTargetConstructor.setAccessible(true);
                        methodHandle = mhLookup.unreflectConstructor(classTargetConstructor);
                    } catch (IllegalAccessException e) {
                        generateFailedMethod(classWriter, intfMethod, FAILED_TO_UNREFLECT + classTargetConstructor);
                        continue;
                    }

                    /* Add MethodHandle into MethodHandles array */
                    methodHandles.add(methodHandle);

                    /* Generate proxy method using MethodHandle */
                    generateConstructorMHMethod(classWriter, intfMethod, classTargetConstructor,
                            proxyClassInternalName, targetClassInternalName,
                            methodHandles.size() - 1);
                } else {
                    generateConstructorMethod(classWriter, intfMethod, classTargetConstructor,
                            proxyClassInternalName, targetClassInternalName);
                }

                /* Start loop from beginning, code below is meant for method proxy */
                continue;
            }

            /* Create method proxy */
            Method targetClassMethod;
            if(targetMethod != null) {
                String methodName = targetMethod.value().isEmpty() ? intfMethod.getName() : targetMethod.value();
                String methodDesc = ""; //targetMethod.desc();
                Class<?>[] params = intfMethod.getParameterTypes();
                Class<?> returnType = intfMethod.getReturnType();

                if(!methodDesc.isEmpty()) {
                    /* TODO: *SIGH* How do I parse this? */
                    throw new UnsupportedOperationException("Using custom descriptor is not supported yet! :(");
                }

                /* Find given method */
                targetClassMethod = findDeclaredMethod(targetClass, methodName, params, returnType);
            } else {
                targetClassMethod = findDeclaredMethod(targetClass, intfMethod);
            }

            /* Ensure target method is present */
            Ensure.notNull(targetClassMethod, FAILED_TO_FIND_METHOD + intfMethod);

            /* Get needed target method info */
            boolean useInstance = false;
            boolean useMH = false;

            if(!Modifier.isStatic(targetClassMethod.getModifiers())) {
                /* Proxy class must use target class instance */
                if(!classMustUseInstance) classMustUseInstance = true;
                useInstance = true;
            }

            if(!Modifier.isPublic(targetClassMethod.getModifiers())) {
                /* Proxy class must use method handles */
                if(!classMustUseMH) classMustUseMH = true;
                useMH = true;
            }

            /* Generate proxy method */
            if(useMH) {
                MethodHandle methodHandle;

                /* Try to look up method handle */
                try {
                    if(!targetClassMethod.isAccessible()) targetClassMethod.setAccessible(true);
                    methodHandle = mhLookup.unreflect(targetClassMethod);
                } catch (IllegalAccessException e) {
                    generateFailedMethod(classWriter, intfMethod, FAILED_TO_UNREFLECT + targetClassMethod);
                    continue;
                }

                /* Add MethodHandle into MethodHandles array */
                methodHandles.add(methodHandle);

                /* Generate proxy method using MethodHandle */
                generateProxyMHMethod(classWriter, intfMethod, targetClassMethod,
                        proxyClassInternalName, targetClassInternalName,
                        useInstance, methodHandles.size() - 1);
            } else {
                generateProxyMethod(classWriter, intfMethod, targetClassMethod,
                        proxyClassInternalName, targetClassInternalName,
                        useInstance);
            }
        }

        /* Check if we have class instance in ClassWrapper */
        if(classMustUseInstance) {
            Ensure.ensureCondition(target.getClassInstance() != null, NO_CLASS_INSTANCE_PRESET);
        }

        /* Set up needed field for class instance reference */
        if(classMustUseInstance) {
            FieldVisitor fv = classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "ref", "L" + targetClassInternalName + ";", null, null);
            fv.visitEnd();
        }

        /* Set up needed field for MethodHandle array */
        if(classMustUseMH) {
            FieldVisitor fv = classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "mh", MH_ARRAY, null, null);
            fv.visitEnd();
        }

        /* Generate constructor */
        generateConstructor(classWriter, classMustUseInstance, classMustUseMH, proxyClassInternalName, targetClassInternalName);

        /* End class writing */
        classWriter.visitEnd();

        /* Load accessor */
        byte[] classData = cw.toByteArray();
        ClassWrapper<?> accessor = Reflect.wrapClass(GCL.defineClass(proxyClassName, classData));

        /* Construct accessor */
        List<TypeWrapper> ctorParams = new ArrayList<>();
        if(classMustUseInstance) ctorParams.add(TypeWrapper.of(targetClass, target.getClassInstance()));
        if(classMustUseMH) ctorParams.add(TypeWrapper.of(MethodHandle[].class,
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
        classNameBuilder.append(COUNTER.computeIfAbsent(intf, k -> new AtomicInteger(0)).getAndIncrement());
        return classNameBuilder.toString();
    }

    @NotNull
    private String getClassName(String name) {
        return name.substring(name.lastIndexOf('.') + 1, name.length());
    }

    /* Finds declared method by name, parameters and return type */
    private Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>[] parameters, Class<?> returnType) {
        return Arrays.stream(clazz.getDeclaredMethods()).filter(method ->
                        method.getName().equals(methodName) &&
                        Arrays.equals(method.getParameterTypes(), parameters) &&
                        method.getReturnType() == returnType
                )
                .findFirst()
                .orElse(null);
    }

    /* Finds method by other method's name, parameters and return type */
    private Method findDeclaredMethod(Class<?> clazz, Method method) {
        return findDeclaredMethod(clazz, method.getName(), method.getParameterTypes(), method.getReturnType());
    }

    /* Finds field */
    private Field findDeclaredField(Class<?> clazz, String fieldName, Class<?> fieldType) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(field ->
                field.getName().equals(fieldName) &&
                field.getType() == fieldType)
                .findFirst()
                .orElse(null);
    }

    /* Finds constructor */
    private Constructor<?> findDeclaredConstructor(Class<?> clazz, Class<?>[] parameters) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(ctor -> Arrays.equals(ctor.getParameterTypes(), parameters))
                .findFirst()
                .orElse(null);
    }

    /* Gets either of string value */
    @Nullable
    private static String either(String one, String two) {
        if(one != null && !one.isEmpty())
            return one;
        if(two != null && !two.isEmpty())
            return two;
        return null;
    }

    /* Gets either of value from annotations */
    @Nullable
    private static <A, B> String either(A one, B two, String value) {
        String val = null;
        if(one != null) val = Reflect.wrapInstance(one).invokeMethod(value, String.class);
        if(val == null && two != null) val = Reflect.wrapInstance(two).invokeMethod(value, String.class);
        return val;
    }
}