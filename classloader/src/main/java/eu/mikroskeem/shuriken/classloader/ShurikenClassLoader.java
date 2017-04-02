package eu.mikroskeem.shuriken.classloader;

import eu.mikroskeem.shuriken.common.streams.ByteArrays;
import org.meteogroup.jbrotli.BrotliStreamDeCompressor;
import org.meteogroup.jbrotli.DeCompressorResult;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Shuriken compressed class loader
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class ShurikenClassLoader extends URLClassLoader {
    static {
        BrotliLibraryLoader.loadBrotli();
    }
    private final Map<String, Class<?>> uncompressedClasses = new HashMap<>();
    private final ClassLoader parentLoader;

    public ShurikenClassLoader(URL[] urls, ClassLoader parent){
        super(urls, parent);
        this.parentLoader = parent;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> foundClass = uncompressedClasses.get(name);
        if(foundClass == null) {
            try {
                /* Try loading from parent */
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                /* Try brotli compressed class and cache it */
                String path = name.replace('.', '/').concat(".class.br");
                InputStream compressedClass = super.getResourceAsStream(path);
                if(compressedClass == null) throw new ClassNotFoundException();

                /* Decompress */
                byte[] compressed = ByteArrays.fromInputStream(compressedClass);
                byte[] decompressed = decompress(compressed);
                foundClass = super.defineClass(name, decompressed, 0, decompressed.length);

                /* Cache */
                uncompressedClasses.put(name, foundClass);
                return foundClass;
            }
        }
        return null;
    }

    /* Decompress compressed class */
    private byte[] decompress(byte[] compressed){
        byte[] uncompressed = new byte[65536]; /* http://stackoverflow.com/a/5497547 */
        int size;
        try(BrotliStreamDeCompressor decompressor = new BrotliStreamDeCompressor()) {
            DeCompressorResult result = decompressor.deCompress(compressed, uncompressed);
            size = result.bytesProduced;
        }
        return Arrays.copyOf(uncompressed, size); /* Strips null bytes */
    }
}
