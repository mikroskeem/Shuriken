package eu.mikroskeem.shuriken.common;

import org.jetbrains.annotations.Contract;

public class SneakyThrow {
    @Contract("_ -> fail")
    public static void throwException(Throwable t) {
        throw SneakyThrow.<RuntimeException>_throwException(t);
    }

    @Contract("_ -> fail")
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T _throwException(Throwable t) throws T {
        assert t != null;
        throw (T) t;
    }
}
