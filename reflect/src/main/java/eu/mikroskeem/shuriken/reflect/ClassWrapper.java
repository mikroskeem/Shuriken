package eu.mikroskeem.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrap class
 *
 * @param <T> Class type
 * @version 0.0.1
 * @author Mark Vainomaa
 */
public final class ClassWrapper<T> {
    /* Method cache */
    private final Map<MethodInfo, Integer> METHOD_INDEX = new HashMap<>();
    private final Map<Integer, Method> METHOD_CACHE = new HashMap<>();

    /* Field cache */
    private final Map<FieldInfo, Integer> FIELD_INDEX = new HashMap<>();
    private final Map<Integer, Field> FIELD_CACHE = new HashMap<>();
    private final Map<Integer, FieldWrapper<?>> FIELDWRAPPER_CACHE = new HashMap<>();

    /* Private constructor */
    private ClassWrapper(Class<T> wrappedClass) {
        if(wrappedClass == null) throw new IllegalStateException("Wrapped class shouldn't be null!");
        this.wrappedClass = wrappedClass;

        /* Build method cache */
        Method[] declaredMethods = wrappedClass.getDeclaredMethods();
        Map<MethodInfo, Method> extraMethods = new HashMap<>();
        for (int i = 0; i <= declaredMethods.length-1; i++) {
            Method method = Reflect.Utils.setMethodAccessible(declaredMethods[i]);
            if(method == null) continue;
            if(method.getReturnType().isPrimitive()) {
                MethodInfo nonPrimitiveMethodInfo = new MethodInfo(
                        method.getName(),
                        PrimitiveType.ensureBoxed(method.getReturnType()),
                        method.getParameterTypes()
                );
                extraMethods.put(nonPrimitiveMethodInfo, method);
            }
            METHOD_INDEX.put(MethodInfo.of(method), i);
            METHOD_CACHE.put(i, method);
        }
        extraMethods.forEach((i, m) -> {
            int index = METHOD_INDEX.size();
            METHOD_INDEX.put(i, index);
            METHOD_CACHE.put(index, m);
        });

        /* Build field cache */
        Field[] declaredFields = wrappedClass.getDeclaredFields();
        Map<FieldInfo, Field> extraFields = new HashMap<>();
        for (int i = 0; i <= declaredFields.length-1; i++) {
            Field field = Reflect.Utils.setFieldAccessible(declaredFields[i]);
            if(field == null) continue;
            if(field.getType().isPrimitive()) {
                FieldInfo nonPrimitiveFieldInfo = new FieldInfo(
                        field.getName(),
                        PrimitiveType.ensureBoxed(field.getType())
                );
                extraFields.put(nonPrimitiveFieldInfo, field);
            }
            FieldInfo fieldInfo = FieldInfo.of(field);
            FIELD_INDEX.put(fieldInfo, i);
            FIELD_CACHE.put(i, field);
        }
        extraFields.forEach((i, f) -> {
            int index = FIELD_INDEX.size();
            FIELD_INDEX.put(i, index);
            FIELD_CACHE.put(index, f);
        });
    }

    private final Class<T> wrappedClass;
    private T classInstance = null;

    /**
     * Construct class with arguments
     *
     * @param args Class arguments
     * @return this {@link ClassWrapper} instance (for chaining)
     * @see Constructor#newInstance(Object...) for exceptions
     */
    public ClassWrapper<T> construct(TypeWrapper... args) {
        /* Simple test to check if instance is already set */
        setClassInstance(null);

        /* Convert TypeWrapper arguments */
        Class<?>[] tArgs = Reflect.Utils.getAllClasses(args);
        Object[] cArgs = Reflect.Utils.getAllObjects(args);

        /* Find constructor */
        Constructor<T> constructor = Reflect.Utils.getDeclaredConstructor(getWrappedClass(), tArgs);

        /* Construct */
        setClassInstance(Reflect.Utils.newInstance(constructor, cArgs));
        return this;
    }

    /**
     * Gets wrapped class
     *
     * @return Wrapped {@link Class}
     */
    @Contract(pure = true)
    public Class<T> getWrappedClass() {
        return wrappedClass;
    }

    /**
     * Gets class instance. May be null, if instance is not set.
     *
     * @return Class instance
     */
    @Nullable
    @Contract(pure = true)
    public T getClassInstance() {
        return classInstance;
    }

    /**
     * Set class instance
     *
     * @param instance Class instance
     * @throws IllegalArgumentException If class instance is already set
     * @return this {@link ClassWrapper} instance (for chaining)
     */
    public ClassWrapper<T> setClassInstance(Object instance) throws IllegalArgumentException {
        if(classInstance != null) throw new IllegalArgumentException("Instance is already set!");
        this.classInstance = wrappedClass.cast(instance);
        return this;
    }

    /**
     * Wrap class
     *
     * @param clazz Class to wrap
     * @param <T> Class type
     * @return Wrapped class
     */
    @NotNull
    @Contract("_ -> !null")
    public static <T> ClassWrapper<T> of(Class<T> clazz) {
        return new ClassWrapper<>(clazz);
    }

    /**
     * Get class field
     *
     * @param fieldName Field's name
     * @param type Field's type class
     * @param <V> Field's type
     * @return {@link FieldWrapper} object or empty, if field wasn't found
     */
    @Contract("null, null -> fail")
    @SuppressWarnings("unchecked")
    public <V> Optional<FieldWrapper<V>> getField(String fieldName, Class<V> type) {
        /* Check arguments */
        if(fieldName == null) throw new IllegalStateException("Field name shouldn't be null!");
        if(type == null) throw new IllegalStateException("Field type shouldn't be null!");

        /* Try to find cached field */
        FieldInfo fieldInfo = new FieldInfo(fieldName, type);

        /* Get field */
        Integer found = FIELD_INDEX.get(fieldInfo);
        Field[] field = new Field[] { found != null ? FIELD_CACHE.get(found) : null };
        field[0] = field[0] != null ? field[0] : findDeclaredField(fieldName, type);
        if(field[0] == null) return Optional.empty();

        /* Wrap field */
        return Optional.of((FieldWrapper<V>)FIELDWRAPPER_CACHE.computeIfAbsent(found,
                k -> MethodHandleFieldWrapper.of(this, field[0], type)));
    }

    /**
     * Get class field
     *
     * @param fieldName Field's name
     * @param type Field's type (in {@link ClassWrapper})
     * @param <V> Field's type
     * @return {@link FieldWrapper} object or empty, if field wasn't found
     */
    public <V> Optional<FieldWrapper<V>> getField(String fieldName, ClassWrapper<V> type) {
        return getField(fieldName, type.getWrappedClass());
    }

    /**
     * Get all available fields in class
     *
     * @return List of fields
     */
    public List<FieldWrapper<?>> getFields() {
        return FIELD_INDEX.values().stream()
                .map(index -> FIELDWRAPPER_CACHE.computeIfAbsent(index, k ->
                        MethodHandleFieldWrapper.of(this, FIELD_CACHE.get(index))))
                .collect(Collectors.toList());
    }

    /**
     * Invokes method and returns
     *
     * @param methodName Method name
     * @param returnType Method's return type class
     * @param args Method's args (pass empty array/no args if there are no args)
     * @param <V> Method's return type
     * @return Method return value
     * @see Method#invoke(Object, Object...) for exceptions
     */
    @Contract("null, null, _ -> fail")
    @SuppressWarnings("unchecked")
    public <V> V invokeMethod(String methodName, Class<V> returnType, TypeWrapper... args) {
        /* Check arguments */
        if(methodName == null) throw new IllegalStateException("Method name shouldn't be null!");
        if(returnType == null) throw new IllegalStateException("Method return type shouldn't be null!");

        /* Convert TypeWrapper types */
        Class<?>[] tArgs = Reflect.Utils.getAllClasses(args);
        Object[] mArgs = Reflect.Utils.getAllObjects(args);

        /* Try to find cached method */
        MethodInfo methodInfo = new MethodInfo(methodName, returnType, tArgs);

        /* Find method */
        Integer found = METHOD_INDEX.get(methodInfo);
        Method method = found != null ? METHOD_CACHE.get(found) : null;
        method = method != null ? method : findDeclaredMethod(methodName, returnType, tArgs);

        /* Do method modifier checks */
        if(!Modifier.isStatic(method.getModifiers()) && getClassInstance() == null) {
            throw new IllegalStateException(String.format("'%s' requires class instance to be set!", method));
        }

        /* Check return type */
        Class<?> returnTypeClazz = method.getReturnType();
        if(returnTypeClazz.isPrimitive()) {
            returnType = PrimitiveType.getBoxed(returnTypeClazz);
        } else if(method.getReturnType() != returnType) {
            throw new IllegalStateException("Method return type didn't match! Expected: " + returnType +
                    ", got: " + method.getReturnType());
        }

        /* Invoke */
        try {
            if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
                return returnType.cast(method.invoke(classInstance, mArgs));
            }
            else {
                method.invoke(classInstance, mArgs);
            }
        } catch (Throwable t) {
            Reflect.Utils.throwException(t);
        }
        return null;
    }

    @Override
    public int hashCode() {
        int result = 61;
        result = result * 61 + (classInstance == null ? 59 : classInstance.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "ClassWrapper<%s>{instance=%s}",
                wrappedClass.toString(),
                classInstance != null? classInstance.toString() : "null"
        );
    }

    /* Finds declared method from given class and its superclasses */
    @NotNull
    private Method findDeclaredMethod(String methodName, Class<?> returnType, Class<?>[] params) {
        Class<?> cls = wrappedClass;
        Method theMethod;
        do {
            theMethod = Arrays.stream(cls.getDeclaredMethods())
                    .filter(m -> {
                        if(methodName.equals(m.getName())) {
                            if(Arrays.equals(m.getParameterTypes(), params)) {
                                if(m.getReturnType() != Object.class) {
                                    Class<?> theReturn = returnType;
                                    Class<?> theMethodReturn = m.getReturnType();
                                    if(theReturn.isPrimitive()) {
                                        theReturn = PrimitiveType.getBoxed(theReturn);
                                    }
                                    if(theMethodReturn.isPrimitive()) {
                                        theMethodReturn = PrimitiveType.getBoxed(theMethodReturn);
                                    }
                                    return theReturn == theMethodReturn;
                                } else {
                                    return true;
                                }
                            }
                        }
                        return false;
                    })
                    .findFirst().orElse(null);
        } while (theMethod == null && (cls = cls.getSuperclass()) != null);
        theMethod = Reflect.Utils.setMethodAccessible(theMethod);
        if(theMethod == null) Reflect.Utils.throwException(new NoSuchMethodException(methodName));
        return theMethod;
    }

    /* Finds declared field from given class and its superclasses */
    @Nullable
    private Field findDeclaredField(String fieldName, Class<?> type) {
        Class<?> cls = wrappedClass;
        Field field;
        do {
            field = Arrays.stream(cls.getDeclaredFields())
                    .filter(f -> fieldName.equals(f.getName()) && (type == Object.class || f.getType() == type))
                    .findFirst().orElse(null);
        } while (field == null && (cls = cls.getSuperclass()) != null);
        field = Reflect.Utils.setFieldAccessible(field);
        return field;
    }

    /* Method info, used in cache */
    private static class MethodInfo {
        MethodInfo(@NotNull String methodName, @NotNull Class<?> returnType,
                   @NotNull Class<?>[] params) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.params = params;
        }

        final String methodName;
        final Class<?> returnType;
        final Class<?>[] params;

        static MethodInfo of(Method method) {
            return new MethodInfo(method.getName(),
                    method.getReturnType(), method.getParameterTypes());
        }

        @Override
        public int hashCode() {
            int result = methodName.hashCode();
            result = 31 * result + returnType.hashCode();
            result = 31 * result + Arrays.hashCode(params);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o)
                return true;
            if(o == null || getClass() != o.getClass())
                return false;

            MethodInfo that = (MethodInfo) o;

            if(!methodName.equals(that.methodName))
                return false;
            if(!returnType.equals(that.returnType))
                return false;
            return Arrays.equals(params, that.params);
        }

        @Override
        public String toString() {
            return "MethodInfo{methodName='" + methodName + '\'' + ", " +
                    "returnType=" + returnType + ", params=" + Arrays.toString(params) + '}';
        }
    }

    /* Field info, used in cache */
    private static class FieldInfo {
        final String fieldName;
        final Class<?> fieldType;

        FieldInfo(@NotNull String fieldName, @NotNull Class<?> fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        static FieldInfo of(Field field) {
            return new FieldInfo(field.getName(), field.getType());
        }

        @Override
        public boolean equals(Object o) {
            if(this == o)
                return true;
            if(o == null || getClass() != o.getClass())
                return false;

            FieldInfo fieldInfo = (FieldInfo) o;

            if(!fieldName.equals(fieldInfo.fieldName))
                return false;
            return fieldType.equals(fieldInfo.fieldType);
        }

        @Override
        public int hashCode() {
            int result = fieldName.hashCode();
            result = 31 * result + fieldType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FieldInfo{" + "fieldName='" + fieldName + '\'' + ", fieldType=" + fieldType + '}';
        }
    }
}
