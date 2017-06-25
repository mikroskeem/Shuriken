package eu.mikroskeem.shuriken.common.function;

/**
 * @author Mark Vainomaa
 */
@FunctionalInterface
public interface CheckedRunnable<T extends Throwable> {
    void run() throws T;
}
