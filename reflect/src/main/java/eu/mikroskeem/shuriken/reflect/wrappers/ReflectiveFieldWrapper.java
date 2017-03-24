package eu.mikroskeem.shuriken.reflect.wrappers;

import eu.mikroskeem.shuriken.reflect.Reflect;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Reflective field wrapper
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectiveFieldWrapper<T> implements FieldWrapper<T> {
    @Getter private final ClassWrapper<?> classWrapper;
    @Getter private final Field field;
    @Getter private final Class<T> type;

    /**
     * Field wrapper
     *
     * @param classWrapper {@link ClassWrapper} instance, where this field is from
     * @param field Backing field
     * @param type Field value type
     * @param <T> Type
     * @return Instance of FieldWrapper
     */
    @Contract("_, !null, !null -> !null")
    public static <T> ReflectiveFieldWrapper<T> of(ClassWrapper<?> classWrapper, Field field, Class<T> type){
        return new ReflectiveFieldWrapper<>(classWrapper, field, type);
    }

    /**
     * Read field
     *
     * @return Field value
     * @throws IllegalAccessException If field isn't accessible
     */
    public T read() throws IllegalAccessException {
        if(!isStatic() && classWrapper.getClassInstance() == null){
            throw new IllegalAccessException(String.format("'%s' requires class instance to be set!", field));
        }
        Class<?> fieldTypeClazz = field.getType();
        Class<T> returnType = type;
        if(fieldTypeClazz.isPrimitive()){
            returnType = PrimitiveType.getBoxed(fieldTypeClazz);
        }
        if(type != Object.class){ // If type is object, don't assert
            assert type == fieldTypeClazz;
        }
        return returnType.cast(field.get(classWrapper.getClassInstance()));
    }

    /**
     * Write field
     *
     * @param value Field value
     * @throws IllegalAccessException If field isn't accessible
     */
    public void write(T value) throws IllegalAccessException {
        if(!isStatic() && classWrapper.getClassInstance() == null){
            throw new IllegalAccessException(String.format("'%s' requires class instance to be set!", field));
        }
        Reflect.QuietReflect.THE_QUIET.hackFinalField(this);
        field.set(classWrapper.getClassInstance(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStatic() {
        return Modifier.isStatic(field.getModifiers());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotation) {
        return Optional.ofNullable(field.getAnnotation(annotation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Annotation> getAnnotations() {
        return Arrays.asList(field.getAnnotations());
    }

    @Override
    public String toString() {
        return String.format(
                "ReflectiveFieldWrapper{field=%s, type=%s, wrapper=%s}",
                field, type, classWrapper
        );
    }
}
