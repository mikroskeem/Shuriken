package eu.mikroskeem.shuriken.instrumentation;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.common.SneakyThrow;
import eu.mikroskeem.shuriken.instrumentation.validate.Validate;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.utils.FunctionalField;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


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

    /**
     * {@link sun.misc.URLClassPath} tools
     */
    public static class URLClassLoaderTools<UCP, UCPLoader> {
        private final ClassWrapper<URLClassLoader> sysCl;
        private final ClassWrapper<UCP> ucp;
        private final ClassWrapper<UCPLoader> ucpLoader;
        private final Map<String, UCPLoader> lmap;
        private final ArrayList<UCPLoader> loaders;

        /**
         * Constructs an {@link sun.misc.URLClassPath} wrapper for {@link URLClassLoader} instance
         */
        @SuppressWarnings({"ConstantConditions", "unchecked"})
        public URLClassLoaderTools(URLClassLoader urlClassLoader) {
            sysCl = Reflect.wrapInstance(urlClassLoader);
            ucp = (ClassWrapper<UCP>) Reflect.getClass("sun.misc.URLClassPath").get();
            ucpLoader = (ClassWrapper<UCPLoader>) Reflect.getClass("sun.misc.URLClassPath$Loader").get();
            ucp.setClassInstance(sysCl.getField("ucp", ucp.getWrappedClass()).get().read());

            /* Try to get loaders map */
            Map<String, UCPLoader> lmap = null;
            Optional<FieldWrapper<HashMap>> theLmap = ucp.getField("lmap", HashMap.class);
            if(theLmap.isPresent()) {
                lmap = (HashMap<String, UCPLoader>) theLmap.get().read();
            } else {
                /* IBM JVM-specific I guess */
                Optional<FieldWrapper<Map>> ibmLmap = ucp.getField("lmap", Map.class);
                if(ibmLmap.isPresent()) {
                    lmap = (Map<String, UCPLoader>) ibmLmap.get().read();
                } else {
                    SneakyThrow.throwException(new NoSuchFieldException("lmap"));
                }
            }

            loaders = (ArrayList<UCPLoader>) ucp.getField("loaders", ArrayList.class).get().read();
            this.lmap = lmap;
        }

        /**
         * Adds an url to {@link sun.misc.URLClassPath}
         * @param url {@link URL} to add
         */
        public void addURL(URL url) {
            Ensure.ensureCondition(url != null, "URL should not be null!");
            Ensure.ensureCondition(url.getProtocol().equals("file"), "Only file:// protocol is supported!");
            ucp.invokeMethod("addURL", void.class, TypeWrapper.of(url));
            UCPLoader ldr = ucp.invokeMethod("getLoader",
                    ucpLoader.getWrappedClass(), TypeWrapper.of(url));
            loaders.add(ldr);
            lmap.put("file://" + url.getFile(), ldr);
        }

        /**
         * Clears lookup cache and forces it on again in {@link sun.misc.URLClassPath}
         */
        public void resetCache() {
            if(System.getProperties().getProperty("java.vendor").contains("Oracle")) { /* Oracle JVM-specific */
                /* Re-enable lookup cache (the addURL will disable it) */
                ucp.getField("lookupCacheEnabled", boolean.class).ifPresent(FunctionalField::writeTrue);

                /* Force cache repopulation */
                ucp.getField("lookupCacheURLs", URL[].class).ifPresent(FunctionalField::writeNull);
                ucp.getField("lookupCacheLoader", ClassLoader.class).ifPresent(FunctionalField::writeNull);
            }
        }
    }
}
