package eu.mikroskeem.shuriken.injector;

import eu.mikroskeem.shuriken.reflect.Reflect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link eu.mikroskeem.shuriken.injector.ShurikenInjector} binder class
 *
 * @author Mark Vainomaa
 */
public class Binder {
    @Getter private final Map<Class<?>, Target<?>> bindings = new HashMap<>();

    /**
     * Bind class
     *
     * @param clazz Class to bind
     * @param <T> Class type
     * @return {@link Target} instance
     */
    public <T> Target<T> bind(Class<T> clazz){
        return new Target<>(this, clazz);
    }

    /**
     * Class bind target
     *
     * @param <T> Class target type
     */
    @RequiredArgsConstructor
    public static class Target<T> {
        private final Binder binder;
        @Getter private final Class<T> bindClass;

        //@Getter private Annotation annotation = null;
        private Class<? extends T> targetClass = null;
        private T instance = null;
        private boolean singleton = false;

        /**
         * Bind class to existing instance. This instance will be injected into fields
         *
         * @param instance Instance
         */
        public void toInstance(T instance){
            this.instance = instance;
            binder.bindings.put(bindClass, this);
        }

        /**
         * Bind class to implementing class. That class will be instantiated for every required field
         *
         * @param clazz Implementing class
         */
        public void to(Class<? extends T> clazz){
            this.targetClass = clazz;
            binder.bindings.put(bindClass, this);
        }

        /**
         * Bind class to implementing class singleton. That class will be instantiated once and given
         * instance is injected to every required field
         *
         * @param clazz Implementing class
         */
        public void toSingleton(Class<? extends T> clazz){
            this.singleton = true;
            this.targetClass = clazz;
            try {
                instance = Reflect.wrapClass(clazz).construct().getClassInstance();
                binder.bindings.put(bindClass, this);
            } catch (Exception e){
                throw new RuntimeException("Failed to configure binding", e);
            }
        }

        /* TODO: finish this
        public Target<T> annotatedWith(Annotation annotation){
            if(this.annotation != null) throw new IllegalStateException("Annotation is already set");
            this.annotation = annotation;
            return this;
        }
        */

        /* Get instance of class */
        T getInstance(){
            if(instance != null){
                return instance;
            } else {
                if(!singleton){
                    try {
                        return Reflect.wrapClass(targetClass).construct().getClassInstance();
                    } catch (Exception e){
                        throw new RuntimeException("Failed to configure binding", e);
                    }
                }
            }
            throw new IllegalStateException("Code shouldn't reach here");
        }
    }

    /**
     * {@link Binder} builder interface
     */
    public interface Builder {
        void configure(Binder binder);
    }
}
