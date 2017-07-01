package eu.mikroskeem.shuriken.reflect;

import org.jetbrains.annotations.Contract;

/**
 * Primitive type map
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
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

    PrimitiveType(Class<?> primitiveClass, Class<?> boxedClass) {
        this.primitiveClass = primitiveClass;
        this.boxedClass = boxedClass;
    }

    private final Class<?> primitiveClass;
    private final Class<?> boxedClass;

    /**
     * Get boxed version of class
     *
     * @param primitiveClass Primitive class
     * @param <T> Primitive class
     * @param <V> Boxed version of given primitive class
     * @return Boxed class
     */
    @Contract("null -> fail")
    @SuppressWarnings("unchecked")
    public static <T, V> Class<V> getBoxed(Class<T> primitiveClass) {
        if(primitiveClass == null) throw new IllegalStateException("Primitive class shouldn't be null!");
        for(PrimitiveType value : PrimitiveType.values()) {
            if(value.getPrimitiveClass() == primitiveClass)
                return (Class<V>) value.getBoxedClass();
        }
        throw new IllegalStateException("Invalid primitive class: " + primitiveClass.getName());
    }

    /**
     * Get primitive version of class
     *
     * @param boxedClass Boxed class
     * @param <T> Boxed class
     * @param <V> Primitive version of given boxed class
     * @return Primitive class
     */
    @Contract("null -> fail")
    @SuppressWarnings("unchecked")
    public static <T, V> Class<V> getUnboxed(Class<T> boxedClass) {
        if(boxedClass == null) throw new IllegalStateException("Boxed class shouldn't be null!");
        for(PrimitiveType value : PrimitiveType.values()) {
            if(value.getBoxedClass() == boxedClass)
                return (Class<V>) value.getPrimitiveClass();
        }
        throw new IllegalStateException("Invalid boxed class: " + boxedClass.getName());
    }

    /**
     * Ensure using always boxed class
     *
     * @param anyClass Type
     * @return Boxed type
     */
    @Contract("null -> fail")
    public static Class<?> ensureBoxed(Class<?> anyClass) {
        if(anyClass == null) throw new IllegalStateException("Class shouldn't be null!");
        if(anyClass.isPrimitive()) {
            return getBoxed(anyClass);
        } else {
            // Sleep > performance. TODO
            return getBoxed(getUnboxed(anyClass));
        }
    }

    /**
     * Gets primitive class of given type
     *
     * @return Primitive class
     */
    public Class<?> getPrimitiveClass() {
        return primitiveClass;
    }

    /**
     * Gets boxed class of given type
     *
     * @return boxed class
     */
    public Class<?> getBoxedClass() {
        return boxedClass;
    }
}
