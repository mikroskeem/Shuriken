package eu.mikroskeem.shuriken.reflect.wrappers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Field wrapper interface
 *
 * @param <T> Type what will be operated with
 * @version 0.0.1
 * @author Mark Vainomaa
 */
public interface FieldWrapper<T> {
    /**
     * Gets field name
     * @return field name
     */
    String getName();

    /**
     * Reads value from field
     * Throws {@link IllegalAccessException} if field reading fails
     *
     * @return Field value
     */
    T read();

    /**
     * Writes value to field
     * Throws {@link IllegalAccessException} if field reading fails
     *
     * @param value Field new value
     */
    void write(T value);

    /**
     * Gets field type
     *
     * @return Field type
     */
    Class<T> getType();

    /**
     * Gets backing field
     *
     * @return Field instance
     */
    Field getField();

    /**
     * If field is static
     *
     * @return true if field is static, false otherwise
     */
    default boolean isStatic() {
        return Modifier.isStatic(getField().getModifiers());
    }

    /**
     * Gets field annotation
     *
     * @param annotation Annotation class
     * @param <A> Annotation type
     * @return Annotation wrapped into Optional. You know what to do
     */
    default <A extends Annotation> Optional<A> getAnnotation(Class<A> annotation) {
        return Optional.ofNullable(getField().getAnnotation(annotation));
    }

    /**
     * Gets all field annotations
     *
     * @return List of field annotations
     */
    default List<? extends Annotation> getAnnotations() {
        return Arrays.asList(getField().getAnnotations());
    }
}
