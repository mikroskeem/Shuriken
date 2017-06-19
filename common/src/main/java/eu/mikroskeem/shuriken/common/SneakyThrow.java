package eu.mikroskeem.shuriken.common;

import org.jetbrains.annotations.Contract;

/**
 * SneakyThrows
 */
public class SneakyThrow {
    /**
     * Good old SneakyThrows! Throws checked exceptions everywhere you want
     *
     * @param t Throwable
     */
    @Contract("_ -> fail")
    public static void throwException(Throwable t) {
        throw SneakyThrow.<RuntimeException>_throwException(t);
    }

    @Contract("_ -> fail")
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T _throwException(Throwable t) throws T {
        throw (T) Ensure.notNull(t, "Throwable should not be null");
    }
}
