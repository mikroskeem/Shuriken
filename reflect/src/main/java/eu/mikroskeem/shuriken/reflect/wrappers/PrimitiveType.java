package eu.mikroskeem.shuriken.reflect.wrappers;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

/**
 * Primitive type map
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@RequiredArgsConstructor
@Getter
public enum PrimitiveType {
    BYTE(byte.class, Byte.class),
    CHAR(char.class, Character.class),
    SHORT(short.class, Short.class),
    INT(int.class, Integer.class),
    LONG(long.class, Long.class),
    FLOAT(float.class, Float.class),
    DOUBLE(double.class, Double.class),
    BOOLEAN(boolean.class, Boolean.class),
    VOID(void.class, Void.class);

    private final Class<?> primitiveClass;
    private final Class<?> boxedClass;

    /**
     * Get boxed version of class
     *
     * @param primitiveClass Primitive class
     * @return Boxed class
     */
    @Contract("null -> fail")
    @SuppressWarnings("unchecked")
    public static <T,V> Class<V> getBoxed(@NonNull Class<T> primitiveClass){
        for(PrimitiveType value : PrimitiveType.values()){
            if(value.getPrimitiveClass() == primitiveClass)
                return (Class<V>) value.getBoxedClass();
        }
        throw new UnsupportedOperationException("Invalid primitive class: " + primitiveClass.getName());
    }

    /**
     * Get primitive version of class
     *
     * @param boxedClass Boxed class
     * @return Primitive class
     */
    @Contract("null -> fail")
    @SuppressWarnings("unchecked")
    public static <T,V> Class<V> getUnboxed(@NonNull Class<T> boxedClass){
        for(PrimitiveType value : PrimitiveType.values()){
            if(value.getBoxedClass() == boxedClass)
                return (Class<V>) value.getPrimitiveClass();
        }
        throw new UnsupportedOperationException("Invalid boxed class: " + boxedClass.getName());
    }
}
