package eu.mikroskeem.test.shuriken.classloader;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.meteogroup.jbrotli.BrotliStreamCompressor;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generate test jar for {@link eu.mikroskeem.shuriken.classloader.ShurikenClassLoader} testing
 *
 * @author Mark Vainomaa
 */
public class GenerateTestJar {
    static {
        BrotliLibraryLoader.loadBrotli();
    }

    @SneakyThrows(IOException.class)
    public static File generateJar(FileWrapper... files){
        File jarFile = File.createTempFile("testjar", ".zip");

        try(
                FileOutputStream fos = new FileOutputStream(jarFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            putPath("eu/mikroskeem/test/shuriken/classloader/classes", zos);
            for (FileWrapper file : files) {
                putFile(file.name, compress(file.data), zos);
            }
            zos.closeEntry();
            zos.finish();
        }
        jarFile.deleteOnExit();
        return jarFile;
    }

    @SneakyThrows(IOException.class)
    private static void putPath(String path, ZipOutputStream zos){
        String[] pathElems = path.split("/");
        String current = "";
        for (String pathElem : pathElems) {
            current = current + pathElem + "/";
            zos.putNextEntry(new ZipEntry(current));
        }
    }

    @SneakyThrows(IOException.class)
    private static void putFile(String path, byte[] data, ZipOutputStream zos){
        zos.putNextEntry(new ZipEntry(path));
        zos.write(data, 0, data.length);
        zos.closeEntry();
    }

    private static byte[] compress(byte[] input){
        BrotliStreamCompressor compressor = new BrotliStreamCompressor();
        return compressor.compressArray(input, true);
    }

    @RequiredArgsConstructor
    public static class FileWrapper {
        private final String name;
        private final byte[] data;
    }
}