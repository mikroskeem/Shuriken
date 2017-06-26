package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.common.SneakyThrow;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Mark Vainomaa
 */
final class GeneratedClassLoader extends ClassLoader {
    @Contract("null, null -> fail")
    Class<?> defineClass(String name, byte[] data) {
        name = Ensure.notNull(name, "Null name").replace('/', '.');
        synchronized(getClassLoadingLock(name)) {
            if (hasClass(name)) throw new IllegalStateException(name + " already defined");
            Class<?> c = this.define(name, Ensure.notNull(data, "Null data"));
            Ensure.ensureCondition(c.getName().equals(name), "class name " + c.getName() + " != requested name " + name);
            return c;
        }
    }

    GeneratedClassLoader(ClassLoader parent) {
        super(parent);
    }

    @NotNull
    private Class<?> define(String name, byte[] data) {
        synchronized (getClassLoadingLock(name)) {
            Ensure.ensureCondition(!hasClass(name), "Already has class: " + name);
            Class<?> c;
            try {
                c = defineClass(name, data, 0, data.length);
            } catch (ClassFormatError e) {
                SneakyThrow.throwException(e);
                return null;
            }
            resolveClass(c);
            return c;
        }
    }

    @Override
    public Object getClassLoadingLock(String name) {
        return super.getClassLoadingLock(name);
    }

    private boolean hasClass(String name) {
        synchronized (getClassLoadingLock(name)) {
            try {
                Class.forName(name);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
