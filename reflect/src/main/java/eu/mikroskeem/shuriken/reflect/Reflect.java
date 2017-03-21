package eu.mikroskeem.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Reflect(){
        throw new RuntimeException("No Reflect instance for you!");
    }

    /**
     * Wrap class into {@link ClassWrapper} instance
     *
     * @param clazz Class
     * @param <T> Class type
     * @return ClassWrapper instance
     */
    public static <T> ClassWrapper<T> wrapClass(Class<T> clazz){
        return ClassWrapper.of(clazz);
    }

    /**
     * Get {@link Class} by name (like <pre>eu.mikroskeem.reflect.Reflect</pre>)
     *
     * @param name Class name
     * @return Class object or empty, if class wasn't found
     */
    @Nullable
    @Contract("null -> fail")
    public static Optional<ClassWrapper<?>> getClass(String name){
        assert name != null;
        return getClass(name, null);
    }

    /**
     * Get {@link Class} by name (like <pre>eu.mikroskeem.reflect.Reflect</pre>)
     *
     * @param name Class name
     * @return Class object or empty, if class wasn't found
     */
    @Nullable
    @Contract("null, null -> fail; null, _ -> fail; !null, null -> _")
    public static Optional<ClassWrapper<?>> getClass(String name, ClassLoader classLoader){
        assert name != null;
        try {
            return Optional.of(ClassWrapper.of(Class.forName(name, true,
                    classLoader!=null?classLoader:ClassLoader.getSystemClassLoader())));
        } catch (ClassNotFoundException e){
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
    public static <T> ClassWrapper<T> construct(ClassWrapper<T> classWrapper, TypeWrapper... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        assert classWrapper != null;
        classWrapper.construct(args);
        return classWrapper;
    }

    /* Bad, real bad */
    static class QuietReflect {
        final static QuietReflect THE_QUIET = new QuietReflect();

        /* You should never do this really */
        @Contract("null, _ -> fail")
        <T> ClassWrapper<T> construct(ClassWrapper<T> classWrapper, TypeWrapper... args) {
            assert classWrapper != null;
            try {
                Class<?>[] tArgs = Stream.of(args).map(TypeWrapper::getType).collect(Collectors.toList()).toArray(new Class[0]);
                Object[] cArgs = Stream.of(args).map(TypeWrapper::getValue).collect(Collectors.toList()).toArray();
                Constructor<T> constructor = classWrapper.getWrappedClass().getConstructor(tArgs);
                classWrapper.setClassInstance(constructor.newInstance(cArgs));
                return classWrapper;
            }
            catch (Exception ignored){}
            return classWrapper;
        }

        /* Same applies to this */
        @Contract("null, null, _ -> fail")
        <T,V> T getField(ClassWrapper<V> classWrapper, String fieldName, Class<T> type) {
            assert classWrapper != null;
            assert fieldName != null;
            try {
                Optional<FieldWrapper<T>> o = classWrapper.getField(fieldName, type);
                if(o.isPresent()) return o.get().read();
            }
            catch (Exception ignored){}
            return null;
        }
    }
}
