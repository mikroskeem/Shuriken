package eu.mikroskeem.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Reflection library
 *
 * @version 0.0.1
 * @author Mark Vainomaa
 */
public class Reflect {
    /** Wrapped class cache */
    private final static Map<ClassEntry, Class<?>> FOUND_CLASS_MAP;

    /** The instance of {@link sun.misc.Unsafe}. Might be null on unsupported JVMs */
    public final static Object THE_UNSAFE;

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
    @Contract("null -> fail")
    @NotNull
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
    @Contract("null -> fail")
    @SuppressWarnings("unchecked")
    public static <T> ClassWrapper<T> wrapInstance(T instance) {
        if(instance == null) throw new IllegalStateException("Instance shouldn't be null!");
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
        Class<?> found = FOUND_CLASS_MAP.compute(new ClassEntry(name, classLoader), (e, c) -> {
            Class<?> clazz;
            if(classLoader != null) {
                clazz = Reflect.Utils.classForName(name, true, classLoader);
            } else {
                clazz = Reflect.Utils.classForName(name);
            }

            if(clazz != null) e.classLoader = clazz.getClassLoader();

            return clazz;
        });

        if(found != null) {
            return Optional.of(wrapClass(found));
        } else {
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

        @Contract("_ -> fail")
        @SuppressWarnings("unchecked")
        private static <T extends Throwable> T _throwException(Throwable t) throws T {
            throw (T) t;
        }

        @Nullable
        static Field setFieldAccessible(Field field) {
            try {
                if (!field.isAccessible()) field.setAccessible(true);
                return field;
            }
            catch (NullPointerException|SecurityException ignored){}
            return null;
        }

        @Nullable
        static <T> Constructor<T> setConstructorAccessible(Constructor<T> constructor) {
            try {
                if (!constructor.isAccessible()) constructor.setAccessible(true);
                return constructor;
            }
            catch (SecurityException ignored){}
            return null;
        }

        @Nullable
        static Method setMethodAccessible(Method method) {
            try {
                if (!method.isAccessible()) method.setAccessible(true);
                return method;
            }
            catch (NullPointerException|SecurityException ignored){}
            return null;
        }

        @NotNull
        @SuppressWarnings("ConstantConditions")
        static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... args) {
            try {
                return setConstructorAccessible(clazz.getDeclaredConstructor(args));
            } catch (Throwable t) {
                throwException(t);
            }
            return null;
        }

        @NotNull
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

        @NotNull
        static Class<?>[] getAllClasses(TypeWrapper[] typeWrappers) {
            return Stream.of(typeWrappers)
                    .map(TypeWrapper::getType)
                    .collect(Collectors.toList())
                    .toArray(new Class[typeWrappers.length]);
        }

        @NotNull
        static Object[] getAllObjects(TypeWrapper[] typeWrappers) {
            return Stream.of(typeWrappers)
                    .map(TypeWrapper::getValue)
                    .collect(Collectors.toList())
                    .toArray(new Object[typeWrappers.length]);
        }

        @Nullable
        static Class<?> classForName(String name) {
            try {
                return Class.forName(name);
            }
            catch (ClassNotFoundException ignored) {}
            return null;
        }

        @Nullable
        static Class<?> classForName(String name, boolean init, ClassLoader classLoader) {
            try {
                return Class.forName(name, init, classLoader);
            }
            catch (ClassNotFoundException ignored) {}
            return null;
        }
    }

    static class ClassEntry {
        ClassEntry(@NotNull String className, ClassLoader classLoader) {
            this.className = className;
            this.classLoader = classLoader;
        }

        final String className;
        ClassLoader classLoader;

        @Override
        public int hashCode() {
            int hashCode = className.hashCode() * 11;
            hashCode = hashCode * (classLoader == null? 1 : classLoader.hashCode());
            return hashCode;
        }

        @Override
        public String toString() {
            return "ClassEntry{className=" + className + ", classLoader=" + classLoader + "}";
        }
    }

    static {
        FOUND_CLASS_MAP = new WeakHashMap<>();
        THE_UNSAFE = Reflect.getClass("sun.misc.Unsafe")
                .flatMap(u -> u.getField("theUnsafe", Object.class))
                .map(FieldWrapper::read).orElse(null);
    }
}
