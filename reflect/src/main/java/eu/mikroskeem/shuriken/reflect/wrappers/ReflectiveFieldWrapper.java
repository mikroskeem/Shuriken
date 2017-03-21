package eu.mikroskeem.shuriken.reflect.wrappers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.Field;

/**
 * Reflective field wrapper
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ReflectiveFieldWrapper<T> implements FieldWrapper<T> {
    private final Object instance;
    @Getter private final Field field;
    @Getter private final Class<T> type;

    /**
     * Field wrapper
     *
     * @param instance Field instance (use null for static access)
     * @param field Backing field
     * @param type Field value type
     * @param <T> Type
     * @return Instance of FieldWrapper
     */
    @Contract("_, !null, !null -> !null")
    public static <T> ReflectiveFieldWrapper<T> of(Object instance, Field field, Class<T> type){
        return new ReflectiveFieldWrapper<>(instance, field, type);
    }

    /**
     * Read field
     *
     * @return Field value
     * @throws IllegalAccessException If field isn't accessible
     */
    @SuppressWarnings("unchecked")
    public T read() throws IllegalAccessException {
        return type.cast(field.get(instance));
    }

    /**
     * Write field
     *
     * @param value Field value
     * @throws IllegalAccessException If field isn't accessible
     */
    public void write(T value) throws IllegalAccessException {
        field.set(instance, value);
    }
}