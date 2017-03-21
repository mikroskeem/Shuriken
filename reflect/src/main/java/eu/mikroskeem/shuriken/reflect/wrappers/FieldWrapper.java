package eu.mikroskeem.shuriken.reflect.wrappers;

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
}
