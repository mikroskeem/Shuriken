package eu.mikroskeem.shuriken.common.function;

import eu.mikroskeem.shuriken.common.SneakyThrow;

import java.util.function.BiFunction;

/**
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface UncheckedBiFunction<A, B, R, E extends Throwable> extends BiFunction<A, B, R> {
    @Override
    default R apply(A a, B b) { try { return actualApply(a, b); } catch (Throwable e) { SneakyThrow.throwException(e); return null; } }

    R actualApply(A a, B b) throws E;
}
