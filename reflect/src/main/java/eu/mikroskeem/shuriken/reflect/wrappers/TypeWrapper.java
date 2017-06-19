package eu.mikroskeem.shuriken.reflect.wrappers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Type container
 * @author Mark vainomaa
 * @version 0.0.1
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TypeWrapper {
    private final Class<?> type;
    private final Object value;

    TypeWrapper(Object value) {
        this.type = value.getClass();
        this.value = value;
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
