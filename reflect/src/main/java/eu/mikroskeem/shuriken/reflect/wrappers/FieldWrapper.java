package eu.mikroskeem.shuriken.reflect.wrappers;

import java.lang.annotation.Annotation;
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
     * Read value from field
     *
     * @return Field value
     * @throws IllegalAccessException if field reading fails
     */
    T read() throws IllegalAccessException;

    /**
     * Write value to field
     *
     * @param value Field new value
     * @throws IllegalAccessException if field writing fails
     */
    void write(T value) throws IllegalAccessException;

    /**
     * Get field type
     *
     * @return Field type
     */
    Class<T> getType();

    /**
     * If field is static
     *
     * @return true if field is static, false otherwise
     */
    boolean isStatic();

    /**
     * Get field annotation
     *
     * @param annotation Annotation class
     * @param <A> Annotation type
     * @return Annotation wrapped into Optional. You know what to do
     */
    <A extends Annotation> Optional<A> getAnnotation(Class<A> annotation);

    /**
     * Get all field annotations
     *
     * @return List of field annotations
     */
    List<? extends Annotation> getAnnotations();
}
