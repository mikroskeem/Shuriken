package eu.mikroskeem.shuriken.reflect.wrappers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

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
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassWrapper<T> {
    @Getter private final Class<T> wrappedClass;
    @Getter private T classInstance = null;

    /**
     * Construct class with arguments
     *
     * @param args Class arguments
     * @return this {@link ClassWrapper} instance (for chaining)
     * @see Constructor#newInstance(Object...) for exceptions
     */
    public ClassWrapper<T> construct(TypeWrapper... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        setClassInstance(null); // Simple test
        /* Convert TypeWrapper arguments */
        Class<?>[] tArgs = Stream.of(args).map(TypeWrapper::getType).collect(Collectors.toList()).toArray(new Class[0]);
        Object[] cArgs = Stream.of(args).map(TypeWrapper::getValue).collect(Collectors.toList()).toArray();
        Constructor<T> constructor = getWrappedClass().getDeclaredConstructor(tArgs);
        /* Make constructor accessible, if it isn't already */
        if(!constructor.isAccessible()) constructor.setAccessible(true);
        setClassInstance(constructor.newInstance(cArgs));
        return this;
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
    @Contract("_ -> !null")
    public static <T> ClassWrapper<T> of(Class<T> clazz){
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
        assert fieldName != null;

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
        if(!field.isAccessible()) field.setAccessible(true);
        return Optional.of(MethodHandleFieldWrapper.of(this, field, type));
    }

    /**
     * Get all available fields in class
     *
     * @return List of fields
     */
    public List<FieldWrapper<?>> getFields(){
        return Stream.of(wrappedClass.getDeclaredFields())
                .map(field -> { if(!field.isAccessible()) field.setAccessible(true); return field; })
                .map(field -> MethodHandleFieldWrapper.of(this, field, field.getType()))
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
     * @throws NoSuchMethodException if method wasn't found
     * @throws InvocationTargetException if invocation didn't succeed
     * @throws IllegalAccessException if reflection cannot be done
     */
    @Contract("null, null, _ -> fail")
    @SuppressWarnings("unchecked")
    public <V> V invokeMethod(String methodName, Class<V> returnType, TypeWrapper... args) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        /* Check arguments */
        assert methodName != null;
        assert returnType != null;

        /* Convert typewrapper */
        Class<?>[] tArgs = Stream.of(args).map(TypeWrapper::getType).collect(Collectors.toList()).toArray(new Class[0]);
        Object[] mArgs = Stream.of(args).map(TypeWrapper::getValue).collect(Collectors.toList()).toArray();

        /* Find method */
        Class<?> cls = wrappedClass;
        Method method;
        final Class<V> _returnType = returnType;
        do {
            method = Arrays.stream(cls.getDeclaredMethods())
                    .filter(m -> {
                        if(methodName.equals(m.getName())) {
                            if(Arrays.equals(m.getParameterTypes(), tArgs)){
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
        if(method == null) throw new NoSuchMethodException(methodName);

        /* Do method modifier checks */
        if(!Modifier.isStatic(method.getModifiers()) && getClassInstance() == null){
            throw new IllegalStateException(String.format("'%s' requires class instance to be set!", method));
        }

        /* Set method accessible, if it is not already */
        if(!method.isAccessible()) method.setAccessible(true);

        /* Check return type */
        Class<?> returnTypeClazz = method.getReturnType();
        if(returnTypeClazz.isPrimitive()){
            returnType = PrimitiveType.getBoxed(returnTypeClazz);
        } else {
            assert method.getReturnType() == returnType;
        }

        /* Invoke */
        if(method.getReturnType() != void.class && method.getReturnType() != Void.class) {
            return returnType.cast(method.invoke(classInstance, mArgs));
        } else {
            method.invoke(classInstance, mArgs);
            return null;
        }
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
