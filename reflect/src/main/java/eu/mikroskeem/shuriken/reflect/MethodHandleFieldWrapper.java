package eu.mikroskeem.shuriken.reflect;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * {@link MethodHandle} based field wrapper
 *
 * @param <T> Field type
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class MethodHandleFieldWrapper<T> implements FieldWrapper<T> {
    private final ClassWrapper<?> classWrapper;
    private final Field field;
    private final Class<T> type;
    private final MethodHandle getter;
    private final MethodHandle setter;

    private MethodHandleFieldWrapper(ClassWrapper<?> classWrapper, Field field, Class<T> type) throws Throwable {
        this.classWrapper = classWrapper;
        this.field = field;
        this.type = type;

        /* Allow modifying final fields */
        hackFinalField();

        /* Set up MethodHandles */
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType getterType;
        MethodType setterType;
        if(isStatic()) {
            getterType = MethodType.methodType(type);
            setterType = MethodType.methodType(void.class, type);
        } else {
            getterType = MethodType.methodType(type, classWrapper.getWrappedClass());
            setterType = MethodType.methodType(void.class, classWrapper.getWrappedClass(), type);
        }
        this.getter = lookup.unreflectGetter(field).asType(getterType);
        this.setter = lookup.unreflectSetter(field).asType(setterType);
    }

    /**
     * Field wrapper
     *
     * @param classWrapper {@link ClassWrapper} instance, where this field is from
     * @param field Backing field
     * @param type Field value type
     * @param <T> Type
     * @return Instance of {@link FieldWrapper}
     */
    @NotNull
    @Contract("_, !null, !null -> !null")
    public static <T> MethodHandleFieldWrapper<T> of(ClassWrapper<?> classWrapper, Field field, Class<T> type) {
        try {
            return new MethodHandleFieldWrapper<>(classWrapper, field, type);
        } catch (Throwable t) {
            Reflect.Utils.throwException(t);
        }
        return null;
    }

    /**
     * Field wrapper
     *
     * @param classWrapper {@link ClassWrapper} instance, where this field is from
     * @param field Backing field

     * @param <T> Type
     * @return Instance of FieldWrapper
     */
    @NotNull
    @Contract("_, !null -> !null")
    @SuppressWarnings("unchecked")
    public static <T> MethodHandleFieldWrapper<T> of(ClassWrapper<?> classWrapper, Field field) {
        try {
            return new MethodHandleFieldWrapper<>(classWrapper, field, (Class<T>)field.getType());
        } catch (Throwable t) {
            Reflect.Utils.throwException(t);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName() {
        return field.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T read() {
        try {
            return read0();
        } catch (Throwable t) {
            Reflect.Utils.throwException(t);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T read0() throws Throwable {
        if(isStatic()) {
            return (T) getter.invoke();
        } else {
            Object instance = classWrapper.getClassInstance();
            if(instance == null)
                throw new IllegalAccessException(String.format("'%s' requires class instance to be set!", field));
            return (T) getter.invoke(instance);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(T value) {
        try {
            write0(value);
        } catch (Throwable t) {
            Reflect.Utils.throwException(t);
        }
    }

    private void write0(T value) throws Throwable {
        if(isStatic()) {
            setter.invoke(value);
        } else {
            Object instance = classWrapper.getClassInstance();
            if(instance == null)
                throw new IllegalAccessException(String.format("'%s' requires class instance to be set!", field));
            setter.invoke(instance, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field getField() {
        return field;
    }

    @Override
    public String toString() {
        return String.format(
                "MethodHandleFieldWrapper{field=%s, type=%s, wrapper=%s}",
                field, type, classWrapper
        );
    }

    private void hackFinalField() {
        int modifiers = getField().getModifiers();
        if(!Modifier.isFinal(modifiers)) return;
        Reflect.wrapInstance(getField()).getField("modifiers", int.class)
                .ifPresent(fw -> fw.write(modifiers & ~Modifier.FINAL));
    }
}
