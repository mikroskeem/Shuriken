package eu.mikroskeem.shuriken.common.function;

import eu.mikroskeem.shuriken.common.SneakyThrow;

import java.util.function.Function;

/**
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface UncheckedFunction<T, R, E extends Throwable> extends Function<T, R> {
    @Override
    default R apply(T t) { try { return actualApply(t); } catch (Throwable e) { SneakyThrow.throwException(e); return null; } }

    R actualApply(T t) throws E;
}
