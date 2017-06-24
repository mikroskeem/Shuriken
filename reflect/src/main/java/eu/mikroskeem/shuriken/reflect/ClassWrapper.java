package eu.mikroskeem.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrap class
 *
 * @param <T> Class type
 * @version 0.0.1
 * @author Mark Vainomaa
 */
public class ClassWrapper<T> {
    /* Private constructor */
    private ClassWrapper(Class<T> wrappedClass) {
        if(wrappedClass == null) throw new IllegalStateException("Wrapped class shouldn't be null!");
        this.wrappedClass = wrappedClass;
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
    public Class<T> getWrappedClass() {
        return wrappedClass;
    }

    /**
     * Gets class instance. May be null, if instance is not set.
     *
     * @return Class instance
     */
    @Nullable
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
    public <I extends T> ClassWrapper<T> setClassInstance(I instance) throws IllegalArgumentException {
        if(classInstance != null) throw new IllegalArgumentException("Instance is already set!");
        this.classInstance = instance;
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
    public <V> Optional<FieldWrapper<V>> getField(String fieldName, Class<V> type) {
        /* Check arguments */
        if(fieldName == null) throw new IllegalStateException("Field name shouldn't be null!");
        if(type == null) throw new IllegalStateException("Field type shouldn't be null!");

        /* Get field */
        Class<?> cls = wrappedClass;
        Field field;
        do {
            field = Arrays.stream(cls.getDeclaredFields())
                    .filter(f -> fieldName.equals(f.getName()) && (type == Object.class || f.getType() == type))
                    .findFirst().orElse(null);
        } while (field == null && (cls = cls.getSuperclass()) != null);
        if(field == null) return Optional.empty();

        /* Set field accessible, if it is not already */
        Reflect.Utils.setFieldAccessible(field);
        return Optional.of(MethodHandleFieldWrapper.of(this, field, type));
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
        return Stream.of(wrappedClass.getDeclaredFields())
                .map(Reflect.Utils::setFieldAccessible)
                .map(field -> MethodHandleFieldWrapper.of(this, field))
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

        /* Convert typewrapper */
        Class<?>[] tArgs = Reflect.Utils.getAllClasses(args);
        Object[] mArgs = Reflect.Utils.getAllObjects(args);

        /* Find method */
        Class<?> cls = wrappedClass;
        Method method;
        final Class<V> _returnType = returnType;
        do {
            method = Arrays.stream(cls.getDeclaredMethods())
                    .filter(m -> {
                        if(methodName.equals(m.getName())) {
                            if(Arrays.equals(m.getParameterTypes(), tArgs)) {
                                if(m.getReturnType() != Object.class) {
                                    Class<?> theReturn = _returnType;
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
        } while (method == null && (cls = cls.getSuperclass()) != null);
        if(method == null) Reflect.Utils.throwException(new NoSuchMethodException(methodName));

        /* Do method modifier checks */
        if(!Modifier.isStatic(method.getModifiers()) && getClassInstance() == null) {
            throw new IllegalStateException(String.format("'%s' requires class instance to be set!", method));
        }

        /* Set method accessible, if it is not already */
        Reflect.Utils.setMethodAccessible(method);

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
    public String toString() {
        return String.format(
                "ClassWrapper<%s>{instance=%s}",
                wrappedClass.toString(),
                classInstance != null? classInstance.toString() : "null"
        );
    }
}
