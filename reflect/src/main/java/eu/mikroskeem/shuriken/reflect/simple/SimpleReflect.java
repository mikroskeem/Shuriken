package eu.mikroskeem.shuriken.reflect.simple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Reflection methods from eu.mikroskeem.utils.reflect library<br>
 * You don't probably want to use this, see {@link eu.mikroskeem.shuriken.reflect.Reflect} instead
 *
 * @version 1.3
 * @author Mark Vainomaa
 */
public class SimpleReflect {
    /**
     * Private constructor, do not use
     */
    private SimpleReflect() {
        throw new RuntimeException("No SimpleReflect instance for you!");
    }

    /**
     * Get declared class method (public,protected,private)
     *
     * @param clazz Class to reflect
     * @param method Method to search
     * @param arguments Method arguments
     * @return Method or null
     */
    @Nullable
    public static Method getMethod(@NotNull Class<?> clazz, @NotNull String method,
                                   Class<?> returnType, Class<?>... arguments) {
        try {
            Method m = clazz.getDeclaredMethod(method, arguments);
            m.setAccessible(true);
            if(m.getReturnType() != returnType) throw new NoSuchMethodException();
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
