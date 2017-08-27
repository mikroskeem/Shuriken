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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.mikroskeem.shuriken.common.Ensure.ensureCondition;
import static eu.mikroskeem.shuriken.common.Ensure.notNull;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.OBJECT;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.generateClassBase;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.generateConstructorProxy;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.generateFailedMethod;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.generateFieldReadMethod;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.generateFieldWriteMethod;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodGenerator.generateMethodProxy;
import static java.util.logging.Level.FINEST;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.V1_8;


/**
 * @author Mark Vainomaa
 */
@SuppressWarnings("unused")
final class MethodReflectorFactory {
    private static final Map<Class<?>, AtomicInteger> COUNTER = new WeakHashMap<>(); /* <Interface Class, Used count> */
    private final static Logger log = Logger.getLogger(MethodReflectorFactory.class.getName());
    private final GeneratedClassLoader GCL = new GeneratedClassLoader(MethodReflectorFactory.class.getClassLoader());
    private final MethodHandles.Lookup mhLookup = MethodHandles.lookup();

    @Nullable private PrintWriter traceClassOutput = null;

    void setTraceClassOutput(@Nullable PrintWriter traceClassOutput) {
        this.traceClassOutput = traceClassOutput;
    }

    @NotNull
    @Contract("null, null, null -> fail")
    <T> T generateReflector(ClassWrapper<?> target, Class<T> intf, Map<String, String> replacements) {
        Ensure.notNull(target, "Target class must not be null!");
        Ensure.notNull(intf, "Interface must not be null!");

        List<MethodHandle> methodHandles = new ArrayList<>();

        /* Reflector proxy class flags */
        int reflectorFlags = 0;
        reflectorFlags |= getTargetModifiers(target);

        Type targetClass = Type.getType(target.getWrappedClass());
        Type interfaceClass = Type.getType(intf);

        String reflectorClassName = generateName(target, reflectorFlags, intf);
        Type reflectorClassType = Type.getType("L" + reflectorClassName.replace('.', '/') + ";");


        //<editor-fold desc="Class generator init">
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor classWriter = MethodReflector.DEBUG ?
                new CheckClassAdapter(traceClassOutput != null ? new TraceClassVisitor(cw, traceClassOutput) : cw, false)
            :
                cw;

        /* Start generating new class */
        classWriter.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
                reflectorClassType.getInternalName(),
                null,
                OBJECT.getInternalName(),
                new String[]{interfaceClass.getInternalName()}
        );
        //</editor-fold>

        /* Iterate over interface class methods */
        for (Method interfaceMethod : intf.getMethods()) {
            int methodFlags = reflectorFlags;
            log.log(FINEST, "Processing interface method {0}", interfaceMethod);
            Type interfaceReturnType = Type.getReturnType(interfaceMethod);
            Type[] interfaceMethodParameters = Type.getArgumentTypes(interfaceMethod);

            //<editor-fold desc="Target information">
            /* Reflector method targets */
            String targetName;
            Type targetReturnType;
            Type[] targetParameters;

            /* Method invoker specific */
            Type targetInterface = null;
            //</editor-fold>

            /* Check for annotations */
            //<editor-fold desc="Annotation checking">
            boolean hasAnnotation = false;
            TargetMethod tMI = null;
            TargetFieldGetter fGI = null;
            TargetFieldSetter fSI = null;
            TargetConstructor tCI = null;
            annotationChecking: {
                if(interfaceMethod.getAnnotations().length < 1)
                    break annotationChecking;
                fGI = interfaceMethod.getAnnotation(TargetFieldGetter.class);
                fSI = interfaceMethod.getAnnotation(TargetFieldSetter.class);
                tMI = interfaceMethod.getAnnotation(TargetMethod.class);
                tCI = interfaceMethod.getAnnotation(TargetConstructor.class);
                String ANNOTATION_ERROR = "Interface method can only have one target or field annotation! ";
                if(fGI != null) { /*ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod);*/ hasAnnotation = true; }
                if(fSI != null) { ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }
                if(tMI != null) { ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }
                if(tCI != null) { ensureCondition(!hasAnnotation, ANNOTATION_ERROR + interfaceMethod); hasAnnotation = true; }
            }
            //</editor-fold>

            //<editor-fold desc="Method generation">
            methodGenerator: {
                //<editor-fold desc="Target preprocessing, finding & processing">
                annotationProcessing: {
                    /* Non-method invokers */
                    if(hasAnnotation && tMI == null) {
                        //<editor-fold desc="Constructor invoker">
                        if(tCI != null) {
                            //<editor-fold desc="Constructor invoker target configuring">
                            methodFlags |= Magic.CTOR_INVOKER;
                            targetName = "<init>";
                            targetParameters = interfaceMethodParameters;
                            targetReturnType = interfaceReturnType;

                            ensureCondition(
                                    interfaceReturnType.equals(targetReturnType)
                                            || isObj(interfaceReturnType)
                                    , "Constructor invoker's return type must match interface method's return type or return java.lang.Object! " + interfaceMethod);

                            if(!tCI.desc().isEmpty()) {
                                String desc = replacePlaceholders(tCI.desc(), replacements);
                                targetParameters = Type.getArgumentTypes(desc);
                                targetReturnType = Type.getReturnType(desc);
                            }

                            /* Constructor return type cannot be Object anymore */
                            ensureCondition(!isObj(targetReturnType)
                                            && !isObj(targetClass) /* People like to do weird stuff */
                                    , "Please override constructor descriptor in annotation, or change method return type: " + interfaceMethod);
                            //</editor-fold>

                            //<editor-fold desc="Constructor finding">
                            Constructor<?> targetConstructor = findConstructor(target.getWrappedClass(), targetParameters);
                            if(targetConstructor == null) {
                                if(interfaceMethod.isDefault()) {
                                    log.log(Level.FINE, "Could not find target constructor for interface method: {0} {1}, but interface default is present.",
                                            new Object[] { target.getWrappedClass(), Arrays.toString(targetParameters) });
                                    continue;
                                }
                                log.log(Level.WARNING, "Could not find target constructor for interface method: {0} {1}",
                                        new Object[] { target.getWrappedClass(), Arrays.toString(targetParameters) });
                                break methodGenerator;
                            }
                            //</editor-fold>

                            /* ** Gather required information */
                            //<editor-fold desc="RETURN_TYPE_PUBLIC flag">
                            if(isAccessible(target.getWrappedClass())) {
                                log.log(FINEST, "Constructable class {0} is public", targetConstructor);
                                methodFlags |= Magic.RETURN_TYPE_PUBLIC;
                            } else {
                                log.log(FINEST, "Constructable class {0} is not public", targetConstructor);
                            }
                            //</editor-fold>

                            //<editor-fold desc="DREFLECTOR_METHOD_USE_METHODHANDLE flag">
                            if(Modifier.isPublic(targetConstructor.getModifiers())) {
                                log.log(FINEST, "Target constructor {0} is public", targetConstructor);
                            } else {
                                log.log(FINEST, "Target constructor {0} is not public, making use of MethodHandle", targetConstructor);
                                methodFlags |= Magic.REFLECTOR_METHOD_USE_METHODHANDLE;
                                reflectorFlags |= Magic.REFLECTOR_CLASS_USE_METHODHANDLE;
                            }
                            //</editor-fold>

                            //<editor-fold desc="Constructor method handle instance fetching">
                            if((methodFlags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
                                try {
                                    if(!targetConstructor.isAccessible()) targetConstructor.setAccessible(true);
                                    MethodHandle methodHandle = mhLookup.unreflectConstructor(targetConstructor);
                                    methodHandles.add(methodHandle);
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE, "Failed to unreflect target {0}: {1}", new Object[] { targetConstructor, e.getMessage() });
                                    break methodGenerator;
                                } catch (Exception e) {
                                    log.log(Level.WARNING, "Failed to generate method", e);
                                    break methodGenerator;
                                }
                            }
                            //</editor-fold>

                            break annotationProcessing;
                        }
                        //</editor-fold>

                        //<editor-fold desc="Field accessor">
                        if(fGI != null || fSI != null) {
                            //<editor-fold desc="Field getter/setter target configuring">
                            if(fGI != null) methodFlags |= Magic.FIELD_GETTER;
                            if(fSI != null) methodFlags |= Magic.FIELD_SETTER;
                            String fieldTypeString = either(fGI, fSI, "type");
                            targetName = replacePlaceholders(either(fGI, fSI, "value"), replacements);
                            ensureCondition(targetName != null && !targetName.isEmpty(), "Field name shouldn't be null or empty!");

                            if((methodFlags & Magic.FIELD_GETTER) != 0) {
                                log.log(FINEST, "Method {0} targets field {1} setter", new Object[]{ interfaceMethod, targetName });
                                ensureCondition(!interfaceReturnType.equals(Type.VOID_TYPE),
                                        "Getters can't return void type! " + interfaceMethod);
                                ensureCondition(interfaceMethodParameters.length == 0,
                                        "Getters can't take any arguments! " + interfaceMethod);
                                targetParameters = new Type[0];
                                targetReturnType = nullOr(replacePlaceholders(fieldTypeString, replacements), Type::getType, interfaceReturnType);
                            } else if((methodFlags & Magic.FIELD_SETTER) != 0) {
                                log.log(FINEST, "Method {0} targets field {1} getter", new Object[]{ interfaceMethod, targetName });
                                ensureCondition(interfaceReturnType.equals(Type.VOID_TYPE),
                                        "Setters can only return void type! " + interfaceMethod);
                                ensureCondition(interfaceMethodParameters.length == 1,
                                        "Setters can only take one argument! " + interfaceMethod);
                                targetParameters = new Type[] { nullOr(replacePlaceholders(fieldTypeString, replacements), Type::getType, interfaceMethodParameters[0]) };
                                targetReturnType = Type.VOID_TYPE;
                            } else {
                                throw new IllegalStateException("Should not reach here");
                            }
                            //</editor-fold>

                            //<editor-fold desc="Field finding">
                            Type fieldType = (methodFlags & Magic.FIELD_GETTER) != 0 ? targetReturnType : targetParameters[0];
                            Field targetField = findField(target.getWrappedClass(), targetName, fieldType);
                            if(targetField == null) {
                                if(interfaceMethod.isDefault()) {
                                    log.log(Level.FINE, "Could not find target field for interface method: {0} {1} {2}, but interface default is present.",
                                            new Object[] { target.getWrappedClass(), targetName, fieldType });
                                    continue;
                                }
                                log.log(Level.WARNING, "Could not find target field for interface method: {0} {1} {2}",
                                        new Object[] { target.getWrappedClass(), targetName, fieldType });
                                break methodGenerator;
                            }
                            //</editor-fold>

                            /* ** Gather required information */
                            //<editor-fold desc="REFLECTOR_METHOD_USE_METHODHANDLE flag">
                            if(Modifier.isPublic(targetField.getModifiers())) {
                                log.log(FINEST, "Target field {0} is public", targetField);
                            } else {
                                log.log(FINEST, "Target field {0} is not public, making use of MethodHandle", targetField);
                                methodFlags |= Magic.REFLECTOR_METHOD_USE_METHODHANDLE;
                                reflectorFlags |= Magic.REFLECTOR_CLASS_USE_METHODHANDLE;
                            }

                            /* Special case for final setter fields */
                            if((methodFlags & Magic.FIELD_SETTER) != 0 && Modifier.isFinal(targetField.getModifiers())) {
                                log.log(FINEST, "Target field {0} is final, removing final modifier and using MethodHandle", targetField);

                                /* Remove final modifier */
                                int modifiers = targetField.getModifiers();
                                Reflect.wrapInstance(targetField).getField("modifiers", int.class)
                                        .ifPresent(fw -> fw.write(modifiers & ~Modifier.FINAL));

                                methodFlags |= Magic.REFLECTOR_METHOD_USE_METHODHANDLE;
                                reflectorFlags |= Magic.REFLECTOR_CLASS_USE_METHODHANDLE;
                            }
                            //</editor-fold>

                            //<editor-fold desc="REFLECTOR_METHOD_USE_INSTANCE flag">
                            if(!Modifier.isStatic(targetField.getModifiers())) {
                                log.log(FINEST, "Target field {0} is not static, using class instance", targetField);
                                methodFlags |= Magic.REFLECTOR_METHOD_USE_INSTANCE;
                                reflectorFlags |= Magic.REFLECTOR_CLASS_USE_INSTANCE;
                            }
                            //</editor-fold>

                            //<editor-fold desc="REFLECTOR_METHOD_USE_METHODHANDLE flag, but checking target type access">
                            if(isAccessible(targetField.getType())) {
                                log.log(FINEST, "Target field {0} type {1} is public", new Object[]{ targetField, targetField.getType() });
                                methodFlags |= Magic.RETURN_TYPE_PUBLIC;
                            } else {
                                log.log(FINEST, "Target field {0} type {1} is not public, using MethodHandle", new Object[]{ targetField, targetField.getType() });
                                if((methodFlags & Magic.FIELD_GETTER) != 0)
                                    targetReturnType = OBJECT;
                                else
                                    targetParameters[0] = OBJECT;
                                methodFlags |= Magic.REFLECTOR_METHOD_USE_METHODHANDLE;
                                reflectorFlags |= Magic.REFLECTOR_CLASS_USE_METHODHANDLE;
                            }
                            //</editor-fold>

                            //<editor-fold desc="Field method handle instance fetching">
                            if((methodFlags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
                                try {
                                    if(!targetField.isAccessible()) targetField.setAccessible(true);
                                    MethodHandle methodHandle;

                                    if((methodFlags & Magic.FIELD_GETTER) != 0) {
                                        methodHandle = mhLookup.unreflectGetter(targetField);
                                    } else if((methodFlags & Magic.FIELD_SETTER) != 0) {
                                        methodHandle = mhLookup.unreflectSetter(targetField);
                                    } else {
                                        throw new IllegalStateException("Should not reach here");
                                    }

                                    methodHandles.add(methodHandle);
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE, "Failed to unreflect target {0}: {1}", new Object[] { targetField, e.getMessage() });
                                    break methodGenerator;
                                } catch (Exception e) {
                                    log.log(Level.WARNING, "Failed to generate method", e);
                                    break methodGenerator;
                                }
                            }
                            //</editor-fold>

                            break annotationProcessing;
                        }
                        //</editor-fold>

                        /* Should not reach here */
                        throw new IllegalStateException("Should not reach here");
                    }

                    //<editor-fold desc="Method invoker">
                    //<editor-fold desc="Method invoker target configuring">
                    targetName = interfaceMethod.getName();
                    targetReturnType = Type.getReturnType(interfaceMethod);
                    targetParameters = Type.getArgumentTypes(interfaceMethod);

                    if(tMI != null) {
                        targetName = tMI.value().isEmpty() ? targetName : replacePlaceholders(tMI.value(), replacements);
                        String desc = replacePlaceholders(tMI.desc(), replacements);
                        targetParameters = tMI.desc().isEmpty() ? targetParameters : Type.getArgumentTypes(desc);
                        targetReturnType = tMI.desc().isEmpty() ? targetReturnType : Type.getReturnType(desc);
                    }
                    //</editor-fold>

                    //<editor-fold desc="Target method finding">
                    Method targetMethod = findMethod(target.getWrappedClass(), targetName, targetParameters, targetReturnType);
                    if(targetMethod == null) {
                        if(interfaceMethod.isDefault()) {
                            log.log(Level.FINE, "Could not find target method for interface method: {0} {1} {2} {3}, but interface default is present.",
                                    new Object[] { target.getWrappedClass(), targetReturnType, targetName, Arrays.toString(targetParameters) });
                            continue;
                        }
                        log.log(Level.WARNING, "Could not find target method for interface method: {0} {1} {2} {3}",
                                new Object[] { target.getWrappedClass(), targetReturnType, targetName, Arrays.toString(targetParameters) });
                        break methodGenerator;
                    }
                    //</editor-fold>

                    //<editor-fold desc="REFLECTOR_METHOD_USE_METHODHANDLE flag">
                    if(Modifier.isPublic(targetMethod.getModifiers())) {
                        /* Check if we can use INVOKEINTERFACE */
                        log.log(FINEST, "Target method {0} is public", targetMethod);
                        //<editor-fold desc="REFLECTOR_METHOD_USE_INVOKEINTERFACE flag">
                        if(Modifier.isInterface(targetMethod.getDeclaringClass().getModifiers())) {
                            log.log(FINEST, "Target method {0} declarer is interface, making use of INVOKEINTERFACE", targetMethod);
                            methodFlags |= Magic.REFLECTOR_METHOD_USE_INVOKEINTERFACE;
                            targetInterface = Type.getType(targetMethod.getDeclaringClass());
                        }
                        //</editor-fold>
                    } else {
                        log.log(FINEST, "Target method {0} is not public, using MethodHandle", targetMethod);
                        methodFlags |= Magic.REFLECTOR_METHOD_USE_METHODHANDLE;
                        reflectorFlags |= Magic.REFLECTOR_CLASS_USE_METHODHANDLE;
                    }
                    //</editor-fold>

                    //<editor-fold desc="RETURN_TYPE_PUBLIC flag (else -> REFLECTOR_CLASS_USE_METHODHANDLE)">
                    if(isAccessible(targetMethod.getReturnType())) {
                        log.log(FINEST, "Target method {0} return type {1} is public", new Object[]{ targetMethod, targetMethod.getReturnType() });
                        methodFlags |= Magic.RETURN_TYPE_PUBLIC;
                    } else {
                        log.log(FINEST, "Target method {0} return type {1} is not public, using MethodHandle", new Object[] { targetMethod, targetMethod.getReturnType() });
                        methodFlags |= Magic.REFLECTOR_METHOD_USE_METHODHANDLE;
                        reflectorFlags |= Magic.REFLECTOR_CLASS_USE_METHODHANDLE;
                    }
                    //</editor-fold>

                    //<editor-fold desc="REFLECTOR_METHOD_USE_INSTANCE flag">
                    if(!Modifier.isStatic(targetMethod.getModifiers())) {
                        log.log(FINEST, "Target method {0} is not static, using class instance", targetMethod);
                        methodFlags |= Magic.REFLECTOR_METHOD_USE_INSTANCE;
                        reflectorFlags |= Magic.REFLECTOR_CLASS_USE_INSTANCE;
                    }
                    //</editor-fold>

                    //<editor-fold desc="Method handle instance fetching">
                    if((methodFlags & Magic.REFLECTOR_METHOD_USE_METHODHANDLE) != 0) {
                        try {
                            if(!targetMethod.isAccessible()) targetMethod.setAccessible(true);
                            MethodHandle methodHandle = mhLookup.unreflect(targetMethod);
                            methodHandles.add(methodHandle);
                        } catch (IllegalAccessException e) {
                            log.log(Level.SEVERE, "Failed to unreflect target {0}: {1}", new Object[] { targetMethod, e.getMessage() });
                            break methodGenerator;
                        } catch (Exception e) {
                            log.log(Level.WARNING, "Failed to generate method", e);
                            break methodGenerator;
                        }
                    }
                    //</editor-fold>

                    //</editor-fold>
                }
                //</editor-fold>

                //<editor-fold desc="Method bytecode generation">
                if((methodFlags & Magic.CTOR_INVOKER) != 0) {
                    generateConstructorProxy(classWriter, interfaceMethod, reflectorClassType, targetClass, targetParameters, methodFlags, methodHandles.size() - 1);
                    continue;
                } else if((methodFlags & Magic.FIELD_GETTER) != 0) {
                    generateFieldReadMethod(classWriter, interfaceMethod, reflectorClassType, targetClass, targetReturnType, targetName, methodFlags, methodHandles.size() - 1);
                    continue;
                } else if((methodFlags & Magic.FIELD_SETTER) != 0) {
                    generateFieldWriteMethod(classWriter, interfaceMethod, reflectorClassType, targetClass, targetParameters[0], targetName, methodFlags, methodHandles.size() - 1);
                    continue;
                } else /* Method reflector */ {
                    generateMethodProxy(classWriter, interfaceMethod, reflectorClassType, targetClass, targetInterface, targetName, targetParameters, targetReturnType, methodFlags, methodHandles.size() -1);
                    continue;
                }
                //</editor-fold>
            }
            //</editor-fold>

            /* Fall-through */
            if(!interfaceMethod.isDefault()) {
                generateFailedMethod(classWriter, interfaceMethod, "Failed to generate implementation for method: " + interfaceMethod);
            }
        }

        //<editor-fold desc="Reflector class base generation and instantiation">
        /* Do validation */
        if((reflectorFlags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0)
            ensureCondition(target.getClassInstance() != null, "Interface targets instance methods, but class instance is not present in ClassWrapper!");

        /* Generate constructor & fields */
        generateClassBase(classWriter, reflectorFlags, reflectorClassType, targetClass);

        /* Load class into memory */
        classWriter.visitEnd();
        byte[] classData = cw.toByteArray();
        ClassWrapper<?> reflector = Reflect.wrapClass(GCL.defineClass(reflectorClassName, classData));

        /* Construct reflector */
        //<editor-fold desc="Argument configuration">
        List<TypeWrapper> ctorParams = new ArrayList<>();
        if((reflectorFlags & Magic.REFLECTOR_CLASS_USE_INSTANCE) != 0) {
            if((reflectorFlags & Magic.TARGET_CLASS_VISIBILITY_PUBLIC) != 0) {
                ctorParams.add(TypeWrapper.of(target.getWrappedClass(), target.getClassInstance()));
            } else {
                ctorParams.add(TypeWrapper.of(Object.class, target.getClassInstance()));
            }
        }
        if((reflectorFlags & Magic.REFLECTOR_CLASS_USE_METHODHANDLE) != 0)
            ctorParams.add(TypeWrapper.of(methodHandles.toArray(new MethodHandle[methodHandles.size()])));
        //</editor-fold>

        @SuppressWarnings("unchecked")
        T reflectorInstance = (T) reflector.construct(ctorParams.toArray(new TypeWrapper[ctorParams.size()])).getClassInstance();
        //</editor-fold>

        return notNull(reflectorInstance, "Reflector instance must not be null!");
    }

    @Contract("null -> fail")
    private int getTargetModifiers(ClassWrapper<?> classWrapper) {
        int modifiers = classWrapper.getWrappedClass().getModifiers();
        return Modifier.isPublic(modifiers) ? Magic.TARGET_CLASS_VISIBILITY_PUBLIC : Magic.TARGET_CLASS_VISIBILITY_PRIVATE;
    }

    @Contract("_, null, null -> fail")
    private <T, U> U nullOr(T value, Function<? super T, ? extends U> mapper, U orElse) {
        notNull(mapper, "mapper must not be null!");
        notNull(orElse, "orElse must not be null!");
        if(value != null) return mapper.apply(value); else return orElse;
    }

    @NotNull
    @Contract("null, _, null -> fail")
    private String generateName(ClassWrapper<?> target, int flags, Class<?> intf) {
        notNull(target, "Target class must not be null!");
        notNull(intf, "Interface class must not be null!");

        StringBuilder classNameBuilder = new StringBuilder();
        classNameBuilder.append(MethodReflector.class.getName());
        classNameBuilder.append('.');
        classNameBuilder.append("$Target$");
        classNameBuilder.append(getClassName(target.getWrappedClass().getName()));
        classNameBuilder.append('$');
        classNameBuilder.append(getClassName(intf.getName()));
        classNameBuilder.append('$');
        classNameBuilder.append(COUNTER.computeIfAbsent(intf, k -> new AtomicInteger(0)).getAndIncrement());
        return classNameBuilder.toString();
    }

    /* Gets either of string value from annotations */
    @Nullable
    @Contract("!null, null, null -> fail")
    private static String either(Object one, Object two, String value) {
        String val = null;
        if(one != null) val = Reflect.wrapInstance(one).invokeMethod(value, String.class);
        if((val == null || val.isEmpty()) && two != null) val = Reflect.wrapInstance(two).invokeMethod(value, String.class);
        return val != null && !val.isEmpty() ? val : null;
    }

    /* Checks if type is java.lang.Object */
    @Contract("null -> fail")
    private static boolean isObj(Type type) {
        return type.equals(MethodGenerator.OBJECT);
    }

    /* Checks if class is accessible as publicly or from package */
    @Contract("null -> fail")
    private static boolean isAccessible(Class<?> clazz) {
        int modifiers = clazz.getModifiers();
        /* Check if class is public */
        return Modifier.isPublic(modifiers);
    }

    /* Gets class name from full class name */
    @NotNull
    @Contract("null -> fail")
    private String getClassName(String name) {
        return name.substring(name.lastIndexOf('.') + 1, name.length());
    }

    /* Replace placeholders in string */
    @Contract("_, null -> fail; null, _ -> null")
    private String replacePlaceholders(String source, Map<String, String> replacements) {
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

        return a[0];
    }


    //<editor-fold desc="Target method/field/constructor finder utilities">
    /* Finds declared method by name, parameters and return type. Probably inefficient as fuck */
    @Nullable
    private static Method findMethod(Class<?> clazz, String methodName, Type[] params, Type returnType) {
        Class<?> scanClass = clazz;
        Method method = null;
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
                                    Arrays.equals(Type.getArgumentTypes(m), params) &&
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
    @Nullable
    private static Field findField(Class<?> clazz, String fieldName, Type fieldType) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(field ->
                field.getName().equals(fieldName)
                        && Type.getType(field.getType()).equals(fieldType))
                .findFirst()
                .orElse(null);
    }

    /* Finds constructor */
    @Nullable
    private static Constructor<?> findConstructor(Class<?> clazz, Type[] parameters) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> Arrays.equals(Type.getType(c).getArgumentTypes(), parameters))
                .findFirst()
                .orElse(null);
    }
    //</editor-fold>
}
