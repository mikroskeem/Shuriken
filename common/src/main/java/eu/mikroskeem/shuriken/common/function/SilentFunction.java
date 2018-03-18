package eu.mikroskeem.shuriken.common.function;

import java.util.function.Function;

/**
 * Silent function interface, throws all exceptions away and returns null afterwards
 * 
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface SilentFunction<T, R> extends Function<T, R> {
    @Override
    default R apply(T t) { try { return actualApply(t); } catch (Exception ignored) { return null; } }

    R actualApply(T t) throws Exception;
}
