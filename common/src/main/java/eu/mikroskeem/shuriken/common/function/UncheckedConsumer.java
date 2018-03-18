package eu.mikroskeem.shuriken.common.function;

import eu.mikroskeem.shuriken.common.SneakyThrow;

import java.util.function.Consumer;

/**
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface UncheckedConsumer<T, E extends Throwable> extends Consumer<T> {
    @Override
    default void accept(T t) { try { actualAccept(t); } catch (Throwable e) { SneakyThrow.throwException(e); } }

    void actualAccept(T t) throws E;
}
