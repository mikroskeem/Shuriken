package eu.mikroskeem.shuriken.classloader;

import eu.mikroskeem.shuriken.common.SneakyThrow;
import eu.mikroskeem.shuriken.common.streams.ByteArrays;
import org.meteogroup.jbrotli.io.BrotliInputStream;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Shuriken compressed class loader
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class ShurikenClassLoader extends URLClassLoader {
    private final Map<String, Class<?>> classes = new HashMap<>();

    public ShurikenClassLoader(URL[] urls) {
        super(urls);
    }

    public ShurikenClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return classes.computeIfAbsent(name, k -> {
            String path = name.replace('.', '/').concat(".class.br");
            InputStream compressedClass = super.getResourceAsStream(path);
            if(compressedClass != null) {
                /* Decompress */
                byte[] decompressed = ByteArrays.fromInputStream(new BrotliInputStream(compressedClass));
                String packageName = name.substring(0, name.lastIndexOf('.'));
                if(getPackage(packageName) == null)
                    definePackage(packageName, null, null, null, null, null, null, null);
                return super.defineClass(name, decompressed, 0, decompressed.length);
            } else {
                /* Try loading usual class */
                try {
                    return super.findClass(name);
                }
                catch (ClassNotFoundException ignored) {}
            }
            SneakyThrow.throwException(new ClassNotFoundException());
            return null;
        });
    }

    /* Load Brotli library */
    static {
        BrotliLibraryLoader.loadBrotli();
    }
}
