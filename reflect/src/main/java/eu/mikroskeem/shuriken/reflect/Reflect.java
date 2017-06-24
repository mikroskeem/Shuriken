package eu.mikroskeem.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Reflection library
 *
 * @version 0.0.1
 * @author Mark Vainomaa
 */
public class Reflect {
    /** The instance of {@link Unsafe}. Might be null on unsupported JVMs */
    public final static Unsafe THE_UNSAFE = Reflect.wrapClass(Unsafe.class).getField("theUnsafe", Unsafe.class)
            .map(FieldWrapper::read).orElse(null);

    /**
     * Private constructor, do not use
     */
    private Reflect() {
        throw new RuntimeException("No Reflect instance for you!");
    }

    /**
     * Wrap class into {@link ClassWrapper} instance
     *
     * @param clazz Class
     * @param <T> Class type
     * @return ClassWrapper instance
     */
    @Contract("_ -> !null")
    public static <T> ClassWrapper<T> wrapClass(Class<T> clazz) {
        return ClassWrapper.of(clazz);
    }

    /**
     * Wrap class instance into {@link ClassWrapper} instance
     *
     * @param instance Instance
     * @param <T> Class type
     * @return ClassWrapper instance
     */
    @SuppressWarnings("unchecked")
    public static <T> ClassWrapper<T> wrapInstance(T instance) {
        return wrapClass((Class<T>)instance.getClass()).setClassInstance(instance);
    }

    /**
     * Get {@link Class} by name (like <pre>eu.mikroskeem.reflect.Reflect</pre>)
     *
     * @param name Class name
     * @return Class object or empty, if class wasn't found
     */
    @Contract("null -> fail")
    public static Optional<ClassWrapper<?>> getClass(String name) {
        return getClass(name, null);
    }

    /**
     * Get {@link Class} by name (like <pre>eu.mikroskeem.reflect.Reflect</pre>)
     *
     * @param name Class name
     * @return Class object or empty, if class wasn't found
     */
    @Contract("null, _ -> fail")
    public static Optional<ClassWrapper<?>> getClass(String name, ClassLoader classLoader) {
        if(name == null) throw new IllegalStateException("Class name shouldn't be null!");
        try {
            if(classLoader != null) {
                return Optional.of(ClassWrapper.of(Class.forName(name, true, classLoader)));
            } else {
                return Optional.of(ClassWrapper.of(Class.forName(name)));
            }
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Construct class with arguments
     *
     * @param classWrapper Class
     * @param args Class (wrapped) arguments
     * @param <T> Type
     * @return Instance of class
     * @see Constructor#newInstance(Object...) for exceptions
     */
    @Contract("null, _ -> fail")
    public static <T> ClassWrapper<T> construct(ClassWrapper<T> classWrapper, TypeWrapper... args) {
        if(classWrapper == null) throw new IllegalStateException("Class wrapper shouldn't be null!");
        classWrapper.construct(args);
        return classWrapper;
    }

    /* Package-private utilities class */
    static class Utils {
        @Contract("_ -> fail")
        static void throwException(Throwable t) {
            throw Utils.<RuntimeException>_throwException(t);
        }

        @SuppressWarnings("unchecked")
        private static <T extends Throwable> T _throwException(Throwable t) throws T {
            throw (T) t;
        }

        static Field setFieldAccessible(Field field) {
            if(!field.isAccessible()) field.setAccessible(true);
            return field;
        }

        static <T> Constructor<T> setConstructorAccessible(Constructor<T> constructor) {
            if(!constructor.isAccessible()) constructor.setAccessible(true);
            return constructor;
        }

        static Method setMethodAccessible(Method method) {
            if(!method.isAccessible()) method.setAccessible(true);
            return method;
        }

        static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... args) {
            try {
                return setConstructorAccessible(clazz.getDeclaredConstructor(args));
            } catch (Throwable t) {
                throwException(t);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        static <T> T newInstance(Constructor<T> constructor, Object... args) {
            try {
                /*if(args.length == 0 && THE_UNSAFE != null) {
                    return (T) THE_UNSAFE.allocateInstance(constructor.getDeclaringClass());
                }*/
                return constructor.newInstance(args);
            } catch (Throwable t) {
                throwException(t);
            }
            return null;
        }

        static Class<?>[] getAllClasses(TypeWrapper[] typeWrappers) {
            return Stream.of(typeWrappers)
                    .map(TypeWrapper::getType)
                    .collect(Collectors.toList())
                    .toArray(new Class[typeWrappers.length]);
        }

        static Object[] getAllObjects(TypeWrapper[] typeWrappers) {
            return Stream.of(typeWrappers)
                    .map(TypeWrapper::getValue)
                    .collect(Collectors.toList())
                    .toArray(new Object[typeWrappers.length]);
        }
    }
}
