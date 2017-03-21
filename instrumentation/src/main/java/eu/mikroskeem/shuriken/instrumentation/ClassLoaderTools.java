package eu.mikroskeem.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.validate.Validate;
import eu.mikroskeem.shuriken.reflect.Reflect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classloader tools
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class ClassLoaderTools {
    /**
     * Private constructor, do not use
     */
    private ClassLoaderTools(){
        throw new RuntimeException("No ClassLoaderTools instance for you!");
    }

    /**
     * Load class from bytearray to existing class loader. Useful when you need to
     * add generated class to classpath
     *
     * @param classLoader Class loader which defines given class
     * @param name Class name
     * @param data Class in bytearray
     * @return Defined class
     * @throws ClassFormatError if class is inconsistent
     */
    @Nullable
    public static Class<?> defineClass(@NotNull ClassLoader classLoader, @NotNull String name, @NotNull byte[] data)
            throws ClassFormatError {
        return Reflect.THE_UNSAFE.defineClass(name, Validate.checkGeneratedClass(data), 0, data.length, classLoader, null);
        /*
        try {
            return Reflect.wrapClass(ClassLoader.class)
                    .setClassInstance(classLoader)
                    .invokeMethod("defineClass", Class.class,
                            of(name), of(byte[].class, data), of(int.class, 0),
                            of(int.class, data.length));
        } catch (InvocationTargetException e) {
            if(e.getTargetException() instanceof ClassFormatError){
                throw (ClassFormatError)e.getTargetException();
            }
        }
        catch (IllegalAccessException|NoSuchMethodException ignored){}
        return null;
        */
    }
}
