package eu.mikroskeem.shuriken.common.function;

import java.util.function.BiFunction;

/**
 * Silent BiFunction interface, throws all exceptions away and returns null afterwards
 * 
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface SilentBiFunction<A, B, R> extends BiFunction<A, B, R> {
    @Override
    default R apply(A a, B b) { try { return actualApply(a, b); } catch (Exception ignored) { return null; } }

    R actualApply(A a, B b) throws Exception;
}
