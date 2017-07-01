package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
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
 * Package-private Method reflector factory, used to generate proxy classes
 *
 * @author Mark Vainomaa
 */
final class MethodReflectorFactory {
    private static final Map<Class<?>, AtomicInteger> COUNTER = new WeakHashMap<>();
    private final GeneratedClassLoader GCL = new GeneratedClassLoader(MethodReflectorFactory.class.getClassLoader());
    private final MethodHandles.Lookup mhLookup = MethodHandles.lookup();

    /* Error messages */
    private final static String NO_CLASS_INSTANCE_PRESET = "Interface targets instance methods, but class instance " +
            "is not present in ClassWrapper!";
    private final static String ANNOTATION_ERROR = "Interface method can only have one target or field annotation! ";
    private final static String FIELD_NAME_IS_NULL = "Field name shouldn't be null or empty!";
    private final static String FAILED_TO_UNREFLECT = "Failed to unreflect target: ";
    private final static String FAILED_TO_FIND_FIELD = "Could not find target field for interface method: ";
    private final static String FAILED_TO_FIND_METHOD = "Could not find target method for interface method: ";
    private final static String FAILED_TO_FIND_CTOR = "Could not find target constructor for interface method: ";
    private final static String SETTER_WRONG_RETURN_TYPE = "Setters can only return void type! ";
    private final static String SETTER_WRONG_PARAM_COUNT = "Setters can only take one argument! ";
    private final static String GETTER_WRONG_RETURN_TYPE = "Getters can't return void type! ";
    private final static String GETTER_WRONG_PARAM_COUNT = "Getters can't take any arguments! ";
    private final static String CTOR_INVOKER_WRONG_RETURN_TYPE = "Constructor invoker's return type must match interface " +
            "method's return type or return java.lang.Object! ";
    private final static String CTOR_TO_USE_OBJECT_PLEASE_OVERRIDE = "Please override constructor descriptor in " +
            "annotation, or change method return type: ";

    @SuppressWarnings("unchecked")
    @Contract("null, null, null -> fail")
    <T> T generateReflector(ClassWrapper<?> target, Class<T> intf, Map<String, String> r) {
        Ensure.notNull(target, "Target class must not be null!");
        Ensure.notNull(intf, "Interface must not be null!");
        List<MethodHandle> methodHandles = new ArrayList<>();
        boolean isTargetPublic = Modifier.isPublic(target.getWrappedClass().getModifiers());
        boolean classMustUseInstance = false;
        boolean classMustUseMH = false;

        /* Proxy class info */
        String proxyClassName = generateName(target, intf);
        Type proxyClassType = Type.getObjectType(unqualifyName(proxyClassName));

        /* Interface class info */
        Type interfaceClassType = Type.getType(intf);

        /* Target class info */
        Class<?> targetClass = target.getWrappedClass();
        Type targetClassType = Type.getType(targetClass);

        /* Set up class writer */
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor classWriter = cw;
        if(MethodReflector.DEBUG) {
            classWriter = new CheckClassAdapter(new TraceClassVisitor(classWriter, new PrintWriter(System.out)), false);
        }

        /* Start generating new class */
        classWriter.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
                proxyClassType.getInternalName(),
                null,
                OBJECT.getInternalName(),
                new String[]{interfaceClassType.getInternalName()}
        );

        /* Iterate through interface methods */
        for(Method interfaceMethod: intf.getMethods()) {
            /* Gather proxy method info */
            boolean interfaceHasDefault = interfaceMethod.isDefault();
            Type[] interfaceParameters = Type.getArgumentTypes(interfaceMethod);
            Type interfaceReturnType = Type.getReturnType(interfaceMethod);

            /* Gather proxy method annotations */
            boolean hasAnnotation = false;
            TargetFieldGetter fieldGetterInfo = interfaceMethod.getAnnotation(TargetFieldGetter.class);
            TargetFieldSetter fieldSetterInfo = interfaceMethod.getAnnotation(TargetFieldSetter.class);
            TargetMethod targetMethodInfo = interfaceMethod.getAnnotation(TargetMethod.class);
            TargetConstructor targetConstructorInfo = interfaceMethod.getAnnotation(TargetConstructor.class);
            if(fieldGetterInfo != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }
            if(fieldSetterInfo != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }
            if(targetMethodInfo != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }
            if(targetConstructorInfo != null) { Ensure.ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }

            /* Check if annotation is present and create method with custom target */
            if(hasAnnotation && targetMethodInfo == null) {
                /* Field getter/setter */
                if(fieldGetterInfo != null || fieldSetterInfo != null) {
                    String fieldName = r(Ensure.notNull(either(fieldGetterInfo, fieldSetterInfo, "value"),
                            FIELD_NAME_IS_NULL), r);
                    String annotationFieldType = r(either(fieldGetterInfo, fieldSetterInfo, "type"), r);
                    boolean isSetter = fieldSetterInfo != null;
                    Type fieldType;

                    /*
                     * Force setters to return void and take only one parameter,
                     * force getters to return type and take no parameters
                     */
                    if(isSetter) {
                        Ensure.ensureCondition(interfaceReturnType.equals(Type.VOID_TYPE),
                                SETTER_WRONG_RETURN_TYPE + interfaceMethod);
                        Ensure.ensureCondition(interfaceParameters.length == 1,
                                SETTER_WRONG_PARAM_COUNT + interfaceMethod);
                        fieldType = Optional.ofNullable(annotationFieldType).map(Type::getType)
                                .orElse(interfaceParameters[0]);
                    } else {
                        Ensure.ensureCondition(!interfaceReturnType.equals(Type.VOID_TYPE),
                                GETTER_WRONG_RETURN_TYPE + interfaceMethod);
                        Ensure.ensureCondition(interfaceParameters.length == 0,
                                GETTER_WRONG_PARAM_COUNT + interfaceMethod);
                        fieldType = Optional.ofNullable(annotationFieldType).map(Type::getType)
                                .orElse(interfaceReturnType);
                    }

                    /* Try to find field */
                    if(MethodReflector.DEBUG)
                        System.out.format("Trying to find field with name '%s', type '%s' from class '%s'%n",
                                fieldName, fieldType, targetClass);
                    Field targetField = findDeclaredField(targetClass, fieldName, fieldType);

                    /* Ensure target field is present */
                    Ensure.notNull(targetField, FAILED_TO_FIND_FIELD + interfaceMethod);

                     /* Get needed target field info */
                    boolean useInstance = false;
                    boolean useMH = false;

                    if(!Modifier.isStatic(targetField.getModifiers())) {
                        /* Proxy class must use target class instance */
                        if(!classMustUseInstance) classMustUseInstance = true;
                        if(MethodReflector.DEBUG)
                            System.out.format("Field '%s' is not static, proxy class must use target class instance%n", targetField);
                        useInstance = true;
                    }

                    if(!isTargetPublic) {
                        /* Must use MethodHandle */
                        if(!classMustUseMH) classMustUseMH = true;
                        if(MethodReflector.DEBUG)
                            System.out.format("Field '%s' declarer class is not public, proxy class must use method handle%n", targetField);
                        useMH = true;
                    }

                    if(!Modifier.isPublic(targetField.getModifiers())) {
                        /* Proxy class must use method handles */
                        if(!classMustUseMH) classMustUseMH = true;
                        if(MethodReflector.DEBUG)
                            System.out.format("Field '%s' is not public, proxy class must use method handle%n", targetField);
                        useMH = true;
                    }

                    if(!Modifier.isPublic(targetField.getType().getModifiers())) {
                        /* Must use MethodHandle, again */
                        if(!classMustUseMH) classMustUseMH = true;
                        fieldType = OBJECT;
                        if(MethodReflector.DEBUG)
                            System.out.format("Field '%s' type is not public, proxy class must use method handle%n", targetField);
                        useMH = true;
                    }

                    if(Modifier.isFinal(targetField.getModifiers())) {
                        /* Proxy class must use method handles */
                        if(!classMustUseMH) classMustUseMH = true;
                        useMH = true;

                        /* Remove final modifier */
                        int modifiers = targetField.getModifiers();
                        Reflect.wrapInstance(targetField).getField("modifiers", int.class)
                                .ifPresent(fw -> fw.write(modifiers & ~Modifier.FINAL));

                        if(MethodReflector.DEBUG)
                            System.out.format("Field '%s' is final, proxy class must use method handle%n", targetField);
                    }

                    /* Generate proxy method */
                    try {
                        int mhIndex = -1;
                        if(useMH) {
                            /* Try to look up method handle */
                            if(!targetField.isAccessible()) targetField.setAccessible(true);
                            MethodHandle methodHandle = isSetter? mhLookup.unreflectSetter(targetField) :
                                    mhLookup.unreflectGetter(targetField);

                            /* Add MethodHandle into MethodHandles array */
                            methodHandles.add(methodHandle);

                            /* Set up mhIndex */
                            mhIndex = methodHandles.size() - 1;
                        }

                        if(isSetter) {
                            /* Generate setter */
                            generateFieldWriteMethod(classWriter, interfaceMethod,
                                    proxyClassType, targetClassType, fieldType, fieldName,
                                    isTargetPublic, useInstance, useMH, mhIndex);
                        } else {
                            /* Generate getter */
                            generateFieldReadMethod(classWriter, interfaceMethod,
                                    proxyClassType, targetClassType, fieldType, fieldName,
                                    isTargetPublic, useInstance, useMH, mhIndex);
                        }
                    } catch (IllegalStateException e) {
                        generateFailedMethod(classWriter, interfaceMethod, e.getMessage());
                    } catch (IllegalAccessException e) {
                        generateFailedMethod(classWriter, interfaceMethod, FAILED_TO_UNREFLECT + targetField);
                    }
                    continue;
                }

                /* ** Constructor invoker */

                /* Ensure interface method returns given class type or Object */
                Ensure.ensureCondition(
                        interfaceReturnType.equals(targetClassType) ||
                        interfaceReturnType.equals(OBJECT),
                        CTOR_INVOKER_WRONG_RETURN_TYPE + interfaceMethod
                );
                Type[] targetParameters = interfaceParameters;
                Type targetReturnType = interfaceReturnType;

                /* Read custom descriptor, if present */
                if(!targetConstructorInfo.desc().isEmpty()) {
                    targetParameters = Type.getArgumentTypes(r(targetConstructorInfo.desc(), r));
                    targetReturnType = Type.getReturnType(r(targetConstructorInfo.desc(), r));
                }

                /* Constructor return type cannot be Object anymore */
                Ensure.ensureCondition(!targetReturnType.equals(OBJECT) &&
                                targetClass != Object.class, /* People like to do weird stuff */
                        CTOR_TO_USE_OBJECT_PLEASE_OVERRIDE + interfaceMethod);

                /* Try to find target constructor */
                if(MethodReflector.DEBUG)
                    System.out.format("Trying to find constructor with parameters '%s' from class '%s'%n",
                            Arrays.toString(targetParameters), targetClass);
                Constructor<?> targetConstructor = findDeclaredConstructor(targetClass, targetParameters);
                Ensure.notNull(targetConstructor, FAILED_TO_FIND_CTOR + interfaceMethod);

                /* Get needed target constructor info */
                boolean useMH = false;

                if(!Modifier.isPublic(targetConstructor.getModifiers())) {
                    /* Proxy class must use method handles */
                    if(!classMustUseMH) classMustUseMH = true;
                    if(MethodReflector.DEBUG)
                        System.out.format(
                                "Constructor '%s' is not public, proxy class must use method handle%n", targetConstructor
                        );
                    useMH = true;
                }

                if(!isTargetPublic) {
                    /* Must use MethodHandle */
                    if(!classMustUseMH) classMustUseMH = true;
                    if(MethodReflector.DEBUG)
                        System.out.format(
                                "Constructor '%s' type is not public, proxy class must use method handle%n", targetConstructor
                        );
                    useMH = true;
                }

                /* Generate proxy method */
                try {
                    int mhIndex = -1;
                    if(useMH) {
                        if(!targetConstructor.isAccessible()) targetConstructor.setAccessible(true);
                        MethodHandle methodHandle = mhLookup.unreflectConstructor(targetConstructor);

                        /* Add MethodHandle into MethodHandles array */
                        methodHandles.add(methodHandle);

                        /* Set up mhIndex */
                        mhIndex = methodHandles.size() - 1;
                    }

                    /* Generate method */
                    generateConstructorProxy(classWriter, interfaceMethod,
                            proxyClassType, targetClassType, targetParameters,
                            isTargetPublic, useMH, mhIndex);
                } catch (IllegalStateException e) {
                    generateFailedMethod(classWriter, interfaceMethod, e.getMessage());
                } catch (IllegalAccessException e) {
                    generateFailedMethod(classWriter, interfaceMethod, FAILED_TO_UNREFLECT + targetConstructor);
                }

                continue;
            }

            /* ** Proceed creating proxy method */

            /* Gather required method parameter/return type info */
            String methodName = targetMethodInfo != null && !targetMethodInfo.value().isEmpty()?
                    r(targetMethodInfo.value(), r)
                    :
                    interfaceMethod.getName();

            Type[] targetParameters = targetMethodInfo != null && !targetMethodInfo.desc().isEmpty()?
                    Type.getArgumentTypes(r(targetMethodInfo.desc(), r))
                    :
                    Type.getArgumentTypes(interfaceMethod);
            Type targetReturnType = targetMethodInfo != null && !targetMethodInfo.desc().isEmpty()?
                    Type.getReturnType(r(targetMethodInfo.desc(), r))
                    :
                    Type.getReturnType(interfaceMethod);

            /* Try to find target method */
            if(MethodReflector.DEBUG)
                System.out.format("Trying to find method with name '%s', with parameters '%s' " +
                                "and return type '%s' from class '%s'%n",
                        methodName, Arrays.toString(targetParameters), targetReturnType, targetClass);
            Method targetMethod = findDeclaredMethod(targetClass, methodName, targetParameters, targetReturnType);

            /* Ensure target method is present */
            if(targetMethod == null && interfaceHasDefault) {
                /* Use interface's default method */
                if(MethodReflector.DEBUG)
                    System.out.format(
                            "Target for '%s' was not found, but default is present, so using interface default%n",
                            interfaceMethod
                    );
                continue;
            }
            Ensure.notNull(targetMethod, FAILED_TO_FIND_METHOD + interfaceMethod);

            /* Get needed target method info */
            boolean useInterface = false;
            boolean useInstance = false;
            boolean useMH = false;

            if(targetMethod.getDeclaringClass().isInterface()) {
                /* INVOKEINTERFACE it is */
                if(MethodReflector.DEBUG)
                    System.out.format(
                            "Method '%s' overrides interface, so using INVOKEINTERFACE%n", targetMethod
                    );
                useInterface = true;
            }

            if(!Modifier.isStatic(targetMethod.getModifiers())) {
                /* Proxy class must use target class instance */
                if(!classMustUseInstance) classMustUseInstance = true;
                if(MethodReflector.DEBUG)
                    System.out.format("Method '%s' is not static, proxy class must use target class instance%n", targetMethod);
                useInstance = true;
            }

            if(!isTargetPublic) {
                /* Must use MethodHandle */
                if(!classMustUseMH) classMustUseMH = true;
                if(MethodReflector.DEBUG)
                    System.out.format("Method '%s' declarer class is not public, proxy class must use method handle%n", targetMethod);
                useMH = true;
            }

            if(!Modifier.isPublic(targetMethod.getReturnType().getModifiers())) {
                /* Must use MethodHandle, again */
                if(!classMustUseMH) classMustUseMH = true;
                targetReturnType = OBJECT;
                if(MethodReflector.DEBUG)
                    System.out.format("Method '%s' return type is not public, proxy class must use method handle%n", targetMethod);
                useMH = true;
            }

            if(!Modifier.isPublic(targetMethod.getModifiers())) {
                /* Proxy class must use method handles */
                if(!classMustUseMH) classMustUseMH = true;
                if(MethodReflector.DEBUG)
                    System.out.format("Method '%s' is not public, proxy class must use method handle%n", targetMethod);
                useMH = true;
            }

            try {
                int mhIndex = -1;
                if(useMH) {
                    /* Try to look up method handle */
                    if(!targetMethod.isAccessible()) targetMethod.setAccessible(true);
                    MethodHandle methodHandle = mhLookup.unreflect(targetMethod);

                    /* Add MethodHandle into MethodHandles array */
                    methodHandles.add(methodHandle);

                    /* Set up mhIndex */
                    mhIndex = methodHandles.size() - 1;
                }

                /* Generate method */
                generateMethodProxy(classWriter, interfaceMethod, proxyClassType, targetClassType,
                        useInterface? Type.getType(targetMethod.getDeclaringClass()) : null,
                        methodName, targetParameters, targetReturnType,
                        isTargetPublic, useInstance,
                        useInterface, useMH, mhIndex);
            } catch (IllegalStateException e) {
                /* Something failed, whoops */
                generateFailedMethod(classWriter, interfaceMethod, e.getMessage());
            } catch (IllegalAccessException e) {
                generateFailedMethod(classWriter, interfaceMethod, FAILED_TO_UNREFLECT + targetMethod);
            }
        }

        /* Check if we have class instance in ClassWrapper */
        if(classMustUseInstance) {
            Ensure.ensureCondition(target.getClassInstance() != null, NO_CLASS_INSTANCE_PRESET);
        }

        /* Generate class base (constructor, fields) */
        generateClassBase(classWriter, classMustUseInstance, classMustUseMH,
                isTargetPublic, proxyClassType, targetClassType);

        /* End class writing */
        classWriter.visitEnd();

        /* Load accessor */
        byte[] classData = cw.toByteArray();
        ClassWrapper<?> accessor = Reflect.wrapClass(GCL.defineClass(proxyClassName, classData));

        /* Construct accessor */
        List<TypeWrapper> ctorParams = new ArrayList<>();
        if(classMustUseInstance) ctorParams.add(TypeWrapper.of(
                isTargetPublic?targetClass:Object.class, target.getClassInstance()));
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

    /* Finds declared method by name, parameters and return type. Probably inefficient as fuck */
    @Nullable
    private Method findDeclaredMethod(Class<?> clazz, String methodName, Type[] params, Type returnType) {
        Class<?> scanClass = clazz;
        Method method;
        /* Scan superclasses */
        do {
            method = Arrays.stream(scanClass.getDeclaredMethods())
                    .filter(m ->
                        methodName.equals(m.getName()) &&
                        Arrays.equals(Type.getArgumentTypes(m), params) &&
                        Type.getType(m.getReturnType()).equals(returnType)
                    )
                    .findFirst().orElse(null);
        } while (method == null && (scanClass = scanClass.getSuperclass()) != null);
        if(method != null) return method;

        /* No interfaces to scan :( */
        if(clazz.getInterfaces().length == 0) return null;

        /* Scan interfaces */
        int i = 0;
        scanClass = clazz.getInterfaces()[i];
        do {
            method = Arrays.stream(scanClass.getDeclaredMethods())
                    .filter(m ->
                        methodName.equals(m.getName()) &&
                        Arrays.equals(m.getParameterTypes(), params) &&
                        Type.getType(m.getReturnType()).equals(returnType) &&
                        (m.isDefault() || Modifier.isStatic(m.getModifiers()))
                    )
                    .findFirst().orElse(null);
            i++;
        } while(method == null && i < scanClass.getInterfaces().length &&
                (scanClass = scanClass.getInterfaces()[i]) != null);
        return method;
    }

    /* Finds field */
    private Field findDeclaredField(Class<?> clazz, String fieldName, Type fieldType) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(field ->
                field.getName().equals(fieldName) &&
                Type.getType(field.getType()).equals(fieldType))
                .findFirst()
                .orElse(null);
    }

    /* Finds constructor */
    private Constructor<?> findDeclaredConstructor(Class<?> clazz, Type[] parameters) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> Arrays.equals(Type.getType(c).getArgumentTypes(), parameters))
                .findFirst()
                .orElse(null);
    }

    /* Gets either of value from annotations */
    @Nullable
    private static <A, B> String either(A one, B two, String value) {
        String val = null;
        if(one != null) val = Reflect.wrapInstance(one).invokeMethod(value, String.class);
        if((val == null || val.isEmpty()) && two != null) val = Reflect.wrapInstance(two).invokeMethod(value, String.class);
        return val != null && !val.isEmpty() ? val : null;
    }

    /* Replace placeholders in string. Sorry for short name, but this method isn't public anyway :) */
    @Nullable
    @Contract("_, null -> fail")
    private String r(String source, Map<String, String> replacements) {
        Ensure.notNull(replacements, "Replacements map shouldn't be null!");
        if(source == null) return null;

        /* Find placeholders */
        List<String> foundPlaceholders = new ArrayList<>();
        StringBuilder lastPlaceholder = null;
        for(char c: source.toCharArray()) {
            if(c == '{' && lastPlaceholder == null) {
                lastPlaceholder = new StringBuilder();
            } else if(lastPlaceholder != null && c != '}') {
                lastPlaceholder.append(c);
            } else if(lastPlaceholder != null) {
                foundPlaceholders.add(lastPlaceholder.toString());
                lastPlaceholder = null;
            }
        }

        /* Amazing hack, wow */
        String[] a = new String[] { source };

        /* Replace placeholders */
        for(String placeholder: foundPlaceholders) {
            replacements.computeIfPresent(placeholder, (k, value) -> {
                a[0] = a[0].replace("{" + k + "}", value);
                return value;
            });
        }

        /* Debug gogogogogo */
        if(MethodReflector.DEBUG)
            System.out.format("Replaced placeholders: '%s' -> '%s'%n", source, a[0]);

        return a[0];
    }
}