package eu.mikroskeem.shuriken.common.function;

import java.util.function.Consumer;

/**
 * Silent consumer interface, throws all exceptions away
 *
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface SilentConsumer<T> extends Consumer<T> {
    @Override
    default void accept(T t) { try { actualAccept(t); } catch (Exception ignored) {} }

    void actualAccept(T t) throws Exception;
}
