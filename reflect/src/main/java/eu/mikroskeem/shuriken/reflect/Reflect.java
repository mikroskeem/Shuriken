package eu.mikroskeem.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Contract;
import sun.misc.Unsafe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.Optional;

import static eu.mikroskeem.shuriken.reflect.Reflect.Callers.check;
import static eu.mikroskeem.shuriken.reflect.Reflect.QuietReflect.THE_QUIET;

/**
 * Reflection library
 *
 * @version 0.0.1
 * @author Mark Vainomaa
 */
public class Reflect {
    public final static Unsafe THE_UNSAFE = THE_QUIET.getField(wrapClass(Unsafe.class), "theUnsafe", Unsafe.class);
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
    @Contract("null, null -> fail; null, _ -> fail; !null, null -> _")
    public static Optional<ClassWrapper<?>> getClass(String name, ClassLoader classLoader) {
        assert name != null;
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
        assert classWrapper != null;
        classWrapper.construct(args);
        return classWrapper;
    }

    /* Bad, real bad */
    public static class QuietReflect {
        public final static QuietReflect THE_QUIET = new QuietReflect();

        /* You should never do this really */
        @Contract("null, _ -> fail")
        @Callers.ensitive
        @SneakyThrows
        public <T> ClassWrapper<T> construct(ClassWrapper<T> classWrapper, TypeWrapper... args) {
            check(QuietReflect.class.getPackage());
            assert classWrapper != null;
            classWrapper.construct(args);
            return classWrapper;
        }

        /* Same applies to this */
        @Contract("null, null, _ -> fail")
        @Callers.ensitive
        @SneakyThrows
        public <T,V> T getField(ClassWrapper<V> classWrapper, String fieldName, Class<T> type) {
            check(QuietReflect.class.getPackage());
            assert classWrapper != null;
            assert fieldName != null;
            Optional<FieldWrapper<T>> o = classWrapper.getField(fieldName, type);
            return o.map(FieldWrapper::read).orElse(null);
        }

        @Contract("null, null, _, null -> fail")
        @Callers.ensitive
        public <T,V> void setField(ClassWrapper<V> classWrapper, String fieldName, Class<T> type, T value) {
            check(QuietReflect.class.getPackage());
            assert classWrapper != null;
            assert fieldName != null;
            assert value != null;
            try {
                Optional<FieldWrapper<T>> o = classWrapper.getField(fieldName, type);
                o.ifPresent(tFieldWrapper -> tFieldWrapper.write(value));
            }
            catch (Exception ignored) {}
        }

        @Callers.ensitive
        public <T> void hackFinalField(FieldWrapper<T> fieldWrapper) {
            check(QuietReflect.class.getPackage());
            Field field = fieldWrapper.getField();
            if (Modifier.isFinal(field.getModifiers())) {
                setField(Reflect.wrapInstance(field), "modifiers", int.class,
                        field.getModifiers() & ~Modifier.FINAL);
            }
        }
    }

    public static class Callers {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @interface ensitive {}

        @Callers.ensitive
        @SuppressWarnings("unchecked")
        public static <T> Class<T> getCaller() {
            Class[] clazz = new Class[] { null };
            StackTraceElement[] stes = new Throwable().getStackTrace();
            StackTraceElement ste = stes[3];
            Reflect.getClass(ste.getClassName()).ifPresent(cw -> clazz[0] = cw.getWrappedClass());
            return clazz[0];
        }

        @Callers.ensitive
        public static void check(Package p) {
            Method m = new Object() {}.getClass().getEnclosingMethod();
            Class<?> caller = getCaller();
            if(m.getAnnotation(ensitive.class) != null && !caller.getPackage().getName().startsWith(p.getName())) {
                throw new IllegalAccessError("Caller sensitive");
            }
        }
    }
}
