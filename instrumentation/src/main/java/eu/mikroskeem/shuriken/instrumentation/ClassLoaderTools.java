package eu.mikroskeem.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetFieldGetter;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetFieldSetter;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetMethod;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static eu.mikroskeem.shuriken.common.Ensure.*;
import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector.newInstance;
import static eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper.of;


/**
 * Classloader tools
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public final class ClassLoaderTools {
    /**
     * Private constructor, do not use
     */
    private ClassLoaderTools() {
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
    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] data) throws ClassFormatError {
        notNull(classLoader, "Classloader shouldn't be null!");
        notNull(name, "Name shouldn't be null!");
        notNull(data, "Class data shouldn't be null!");
        if(Reflect.THE_UNSAFE != null)
            return Reflect.THE_UNSAFE
                    .invokeMethod("defineClass", Class.class,
                            of(name), of(byte[].class, data), of(int.class, 0),
                            of(int.class, data.length), of(ClassLoader.class, classLoader),
                            of(ProtectionDomain.class, null));
        return Reflect.wrapInstance(classLoader)
                .invokeMethod("defineClass", Class.class,
                        of(name), of(byte[].class, data), of(int.class, 0),
                        of(int.class, data.length));
    }

    /**
     * {@link sun.misc.URLClassPath} tools
     */
    public static class URLClassLoaderTools {
        private final ClassWrapper<URLClassLoader> cl;
        private final UCPAccessor ucpAccessor;

        /**
         * Constructs an {@link sun.misc.URLClassPath} wrapper for {@link URLClassLoader} instance
         */
        @SuppressWarnings("unchecked")
        public URLClassLoaderTools(URLClassLoader urlClassLoader) {
            cl = Reflect.wrapInstance(urlClassLoader);
            ClassWrapper<?> ucp = Reflect.getClassThrows("sun.misc.URLClassPath");
            ucp.setClassInstance(ensurePresent(cl.getField("ucp", ucp.getWrappedClass()),
                    "Could not get URLClassPath instance").read());

            /* Set up URLClassPath accessor */
            ucpAccessor = newInstance(ucp, UCPAccessor.class).getReflector();

            /* Try to get loaders map */
            notNull(Optional.ofNullable((Map<String, Object>) ucpAccessor.getLmap())
                    .orElse(ucpAccessor.getIBMLmap()), "Could not find 'lmap' field!");
        }

        /**
         * Adds an url to {@link sun.misc.URLClassPath}
         * @param url {@link URL} to add
         */
        @Contract("null -> fail")
        public void addURL(URL url) {
            notNull(url, "URL should not be null!");
            ensureCondition(url.getProtocol().equals("file"), "Only file:// protocol is supported!");

            ucpAccessor.addURL(url);
            Object ucpLoader = ucpAccessor.getLoader(url);
            ucpAccessor.getLoaders().add(ucpLoader);
            Optional.ofNullable((Map<String, Object>) ucpAccessor.getLmap())
                    .orElse(ucpAccessor.getIBMLmap())
                    .put("file://" + url.getFile(), ucpLoader);
        }

        /**
         * Clears lookup cache and forces it on again in {@link sun.misc.URLClassPath}
         */
        public void resetCache() {
            if(System.getProperties().getProperty("java.vendor").contains("Oracle")) { /* Oracle JVM-specific */
                /* Re-enable lookup cache (the addURL will disable it) */
                ucpAccessor.setLookupCacheEnabled(false);

                /* Force cache repopulation */
                ucpAccessor.setLookupCacheURLs(null);
                ucpAccessor.setLookupCacheLoader(null);
            }
        }

        public interface UCPAccessor {
            void addURL(URL url);

            @TargetMethod(desc = "(Ljava/net/URL;)Lsun/misc/URLClassPath$Loader;")
            Object getLoader(URL url);

            @TargetFieldGetter("lmap") default HashMap<String, Object> getLmap() { return null; }
            @TargetFieldGetter("lmap") default Map<String, Object> getIBMLmap() { return null; }
            @TargetFieldGetter("loaders") ArrayList<Object> getLoaders();

            @TargetFieldSetter("lookupCacheEnabled") default void setLookupCacheEnabled(boolean value) {
                throw new RuntimeException("Could not find 'lookupCacheEnabled' field!");
            }
            @TargetFieldSetter("lookupCacheURLs") default void setLookupCacheURLs(URL[] urls) {
                throw new RuntimeException("Could not find 'lookupCacheURLs' field!");
            }
            @TargetFieldSetter("lookupCacheLoader") default void setLookupCacheLoader(ClassLoader classLoader) {
                throw new RuntimeException("Could not find 'lookupCacheLoader' field!");
            }
        }
    }
}
