package eu.mikroskeem.shuriken.injector;

/**
 * Injector interface
 *
 * @author Mark Vainomaa
 */
public interface Injector {
    /**
     * Instantiate object via no-args constructor
     *
     * @param clazz Class to instatiate
     * @param <T> Class type
     * @return Instatiated class with injected fields
     */
    <T> T getInstance(Class<T> clazz);

    /**
     * Inject existing fields annotated
     * with {@link javax.inject.Inject}
     *
     * @param instance Instance to inject
     * @param <T> Instance type
     * @return Injected instance (for chaining)
     */
    <T> T injectMembers(T instance);
}
