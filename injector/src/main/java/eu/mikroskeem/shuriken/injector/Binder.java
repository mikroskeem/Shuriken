package eu.mikroskeem.shuriken.injector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.Reflect;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link eu.mikroskeem.shuriken.injector.ShurikenInjector} binder class
 *
 * @author Mark Vainomaa
 */
public class Binder {
    private final Map<Class<?>, Target<?>> bindings = new HashMap<>();

    /**
     * Binds class
     *
     * @param clazz Class to bind
     * @param <T> Class type
     * @return {@link Target} instance
     */
    public <T> Target<T> bind(Class<T> clazz) {
        return new Target<>(this, clazz);
    }

    /**
     * Gets bindings
     * 
     * @return Map of classes and targets
     */
    Map<Class<?>, Target<?>> getBindings() {
        return bindings;
    }

    /**
     * Class bind target
     *
     * @param <T> Class target type
     */
    public static class Target<T> {
        private final Binder binder;
        private final Class<T> bindClass;
        
        private Target(Binder binder, Class<T> bindClass) {
            this.binder = Ensure.notNull(binder, "Binder shouldn't be null!");
            this.bindClass = Ensure.notNull(bindClass, "Bind class shouldn't be null!");
        }

        //@Getter private Annotation annotation = null;
        private Class<? extends T> targetClass = null;
        private T instance = null;
        private boolean singleton = false;

        /**
         * Gets bind class
         *
         * @return bind class
         */
        public Class<T> getBindClass() {
            return bindClass;
        }

        /**
         * Binds class to existing instance. This instance will be injected into fields
         *
         * @param instance Instance
         */
        @Contract("null -> fail")
        public void toInstance(T instance) {
            this.instance = Ensure.notNull(instance, "Instance shouldn't be null!");
            binder.bindings.put(bindClass, this);
        }

        /**
         * Binds class to implementing class. That class will be instantiated for every required field
         *
         * @param clazz Implementing class
         */
        @Contract("null -> fail")
        public void to(Class<? extends T> clazz) {
            this.targetClass = Ensure.notNull(clazz, "Class shouldn't be null!");
            binder.bindings.put(bindClass, this);
        }

        /**
         * Binds class to implementing class singleton. That class will be instantiated once and given
         * instance is injected to every required field
         *
         * @param clazz Implementing class
         */
        @Contract("null -> fail")
        public void toSingleton(Class<? extends T> clazz) {
            this.singleton = true;
            this.targetClass = Ensure.notNull(clazz, "Class shouldn't be null!");
            try {
                instance = Reflect.wrapClass(clazz).construct().getClassInstance();
                binder.bindings.put(bindClass, this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure binding", e);
            }
        }

        /* TODO: finish this
        public Target<T> annotatedWith(Annotation annotation) {
            if(this.annotation != null) throw new IllegalStateException("Annotation is already set");
            this.annotation = annotation;
            return this;
        }
        */

        /* Get instance of class */
        T getInstance() {
            if(instance == null) {
                if(!singleton) {
                    try {
                        return Reflect.wrapClass(targetClass).construct().getClassInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure binding", e);
                    }
                }
            }
            return instance;
        }
    }

    /**
     * {@link Binder} builder interface
     */
    public interface Builder {
        void configure(Binder binder);
    }
}
