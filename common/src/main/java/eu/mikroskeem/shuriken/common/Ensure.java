package eu.mikroskeem.shuriken.common;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static eu.mikroskeem.shuriken.common.SneakyThrow.throwException;
import static eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper.of;

/**
 * Ensure
 */
public class Ensure {
    public static void ensureCondition(boolean condition, Class<? extends Exception> exception, TypeWrapper... args){
        if(!condition){
            try {
                throw Reflect.construct(Reflect.wrapClass(exception), args).getClassInstance();
            } catch (Throwable e){
                throwException(e);
            }
        }
    }

    @Contract("null, _ -> fail; !null, _ -> !null")
    public static <T> T notNull(T ref, @Nullable String errorMessage) {
        ensureCondition(ref != null, NullPointerException.class, of(""+errorMessage));
        return ref;
    }

    @NotNull
    @Contract("null, _ -> fail")
    public static <T> T ensurePresent(@NonNull Optional<T> optional, @Nullable String errorMessage){
        ensureCondition(optional.isPresent(), NullPointerException.class, of(""+errorMessage));
        return optional.get();
    }
}