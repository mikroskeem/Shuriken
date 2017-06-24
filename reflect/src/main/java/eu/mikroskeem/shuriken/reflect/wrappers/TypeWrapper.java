package eu.mikroskeem.shuriken.reflect.wrappers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Type container
 * @author Mark vainomaa
 * @version 0.0.1
 */
public class TypeWrapper {
    private final Class<?> type;
    private final Object value;

    private TypeWrapper(Class<?> type, Object value) {
        this.type = type;
        this.value = value;
    }

    private TypeWrapper(Object value) {
        this.type = value.getClass();
        this.value = value;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @NotNull
    @Contract("null -> fail")
    public static TypeWrapper of(Object value) {
        return new TypeWrapper(value);
    }

    @NotNull
    @Contract("!null, _ -> !null; null, _ -> fail")
    public static TypeWrapper of(Class<?> type, Object value) {
        if(type == null) throw new IllegalStateException("Type must not be null");
        return new TypeWrapper(type, value);
    }

    @Override
    public String toString() {
        return String.format("TypeWrapper{type=%s, value=%s}", type, value);
    }
}
