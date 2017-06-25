package eu.mikroskeem.test.shuriken.classloader;

import eu.mikroskeem.shuriken.common.SneakyThrow;
import eu.mikroskeem.shuriken.common.data.Pair;
import org.meteogroup.jbrotli.BrotliStreamCompressor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @author Mark Vainomaa
 */
public class Utils {
    public static Path generateTestJar(Pair<String, byte[]>... files) {
        Path jarFile;
        try {
            jarFile = Files.createTempFile("testjar", ".jar");
            try(ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jarFile))) {
                Map<String, List<Pair<String, byte[]>>> filesMap = new HashMap<>();
                for (Pair<String, byte[]> file : files) {
                    filesMap.compute(file.getKey(), (k, data) -> {
                        List<Pair<String, byte[]>> fileData = data != null ? data : new ArrayList<>();
                        fileData.add(new Pair<>(file.getKey(), compress(file.getValue())));
                        return fileData;
                    });
                }

                filesMap.forEach((path, data) -> {
                    putPath(Paths.get(path).getParent().toString(), zos);
                    data.forEach(file -> putFile(file.getKey(), file.getValue(), zos));
                });

                zos.closeEntry();
                zos.finish();
            }
        } catch (Throwable e) {
            SneakyThrow.throwException(e);
            return null;
        }
        //jarFile.toFile().deleteOnExit();
        return jarFile;
    }

    public static void putPath(String path, ZipOutputStream zos) {
        try {
            String[] pathElems = path.split("/");
            String current = "";
            for (String pathElem : pathElems) {
                current = current + pathElem + "/";
                zos.putNextEntry(new ZipEntry(current));
            }
        } catch (Throwable e) {
            SneakyThrow.throwException(e);
        }
    }

    public static void putFile(String path, byte[] data, ZipOutputStream zos) {
        try {
            zos.putNextEntry(new ZipEntry(path));
            zos.write(data, 0, data.length);
            zos.closeEntry();
        } catch (Throwable e) {
            SneakyThrow.throwException(e);
        }
    }


    public static byte[] compress(byte[] input) {
        BrotliStreamCompressor compressor = new BrotliStreamCompressor();
        return compressor.compressArray(input, true);
    }
}
