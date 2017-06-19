package eu.mikroskeem.shuriken.reflect.wrappers;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import static eu.mikroskeem.shuriken.reflect.Reflect.QuietReflect.THE_QUIET;

/**
 * {@link MethodHandle} based field wrapper
 *
 * @param <T> Field type
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class MethodHandleFieldWrapper<T> implements FieldWrapper<T> {
    @Getter private final ClassWrapper<?> classWrapper;
    @Getter private final Field field;
    @Getter private final Class<T> type;
    private final MethodHandle getter;
    private final MethodHandle setter;

    @SneakyThrows(IllegalAccessException.class)
    private MethodHandleFieldWrapper(ClassWrapper<?> classWrapper, Field field, Class<T> type) {
        this.classWrapper = classWrapper;
        this.field = field;
        this.type = type;
        THE_QUIET.hackFinalField(this);
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
     * @return Instance of FieldWrapper
     */
    @NotNull
    @Contract("_, !null, !null -> !null")
    public static <T> MethodHandleFieldWrapper<T> of(ClassWrapper<?> classWrapper, Field field, Class<T> type) {
        return new MethodHandleFieldWrapper<>(classWrapper, field, type);
    }

    /**
     * Gets field name
     *
     * @return field name
     */
    @Override
    @NotNull
    public String getName() {
        return field.getName();
    }

    /**
     * Read field
     * Throws {@link IllegalAccessException} If field isn't accessible or class instance is not set
     *                                       (only if field is instance field)
     *
     * @return  Field value
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T read() {
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
     * Write field
     * Throws {@link IllegalAccessException} If field isn't accessible or class instance is not set
     *                                       (only if field is instance field)
     *
     * @param value Field value
     */
    @Override
    @SneakyThrows
    public void write(T value) {
        if(isStatic()) {
            setter.invoke(value);
        } else {
            Object instance = classWrapper.getClassInstance();
            if(instance == null)
                throw new IllegalAccessException(String.format("'%s' requires class instance to be set!", field));
            setter.invoke(instance, value);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "MethodHandleFieldWrapper{field=%s, type=%s, wrapper=%s}",
                field, type, classWrapper
        );
    }
}
