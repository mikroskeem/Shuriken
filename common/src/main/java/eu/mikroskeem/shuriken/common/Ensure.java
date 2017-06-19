package eu.mikroskeem.shuriken.common;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static eu.mikroskeem.shuriken.common.SneakyThrow.throwException;
import static eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper.of;

/**
 * Ensure conditions
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class Ensure {
    /**
     * Ensure that condition is true
     *
     * @param condition Condition
     * @param exception Exception what will be thrown, if condition isn't true
     * @param args Exception arguments
     */
    @Contract("false, _, _ -> fail")
    public static void ensureCondition(boolean condition, Class<? extends Exception> exception, TypeWrapper... args) {
        if(!condition) throwException(Reflect.construct(Reflect.wrapClass(exception), args).getClassInstance());
    }

    /**
     * Throws {@link IllegalStateException} if condition is not true
     *
     * @param condition Condition to assert
     * @param text Message in {@link IllegalStateException}
     */
    @Contract("false, _ -> fail")
    public static void ensureCondition(boolean condition, String text) {
        if(!condition) throw new IllegalStateException(text);
    }

    /**
     * Check if reference is not null
     *
     * @param ref Object reference
     * @param errorMessage NullPointerException message
     * @param <T> Reference type
     * @return Passed reference
     */
    @Contract("null, _ -> fail; !null, _ -> !null")
    public static <T> T notNull(T ref, @Nullable String errorMessage) {
        ensureCondition(ref != null, NullPointerException.class, of(""+errorMessage));
        return ref;
    }

    /**
     * Ensure that {@link Optional} value is present
     *
     * @param optional Optional to check
     * @param errorMessage NullPointerException message
     * @param <T> Value type wrapped inside {@link Optional}
     * @return Optional value
     */
    @SuppressWarnings({"ConstantConditions", "OptionalUsedAsFieldOrParameterType"})
    @NotNull
    @Contract("null, _ -> fail")
    public static <T> T ensurePresent(Optional<T> optional, @Nullable String errorMessage) {
        ensureCondition(
                notNull(optional, "Optional parameter shouldn't be null!").isPresent(),
                NullPointerException.class,
                of(""+errorMessage)
        );
        return optional.get();
    }
}