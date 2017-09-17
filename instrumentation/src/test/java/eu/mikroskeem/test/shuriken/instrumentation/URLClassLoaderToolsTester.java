package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.common.ToURL;
import eu.mikroskeem.shuriken.instrumentation.ClassLoaderTools;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * @author Mark Vainomaa
 */
public class URLClassLoaderToolsTester {
    @BeforeAll
    public static void setupMethodReflector() {
        MethodReflector.DEBUG = true;
    }

    @AfterAll
    public static void cleanMethodReflector() {
        MethodReflector.DEBUG = false;
    }

    @Test
    public void testUCLTools() throws Exception {
        ClassLoader ucl = ClassLoader.getSystemClassLoader();
        ClassLoaderTools.URLClassLoaderTools uclTools = ucl instanceof URLClassLoader ?
                new ClassLoaderTools.URLClassLoaderTools((URLClassLoader) ucl)
            :
                new ClassLoaderTools.URLClassLoaderTools(ucl);

        Assertions.assertThrows(IllegalStateException.class, () ->
                uclTools.addURL(new URL("https://mikroskeem.eu/stuff/fernflower.jar"))
        );
        uclTools.addURL(ToURL.to(downloadTestJar()));
        uclTools.resetCache();
    }


    private Path downloadTestJar() throws Exception {
        /* aopalliance library for test, hopefully this won't disappear */
        String url = "https://repo.maven.apache.org/maven2/aopalliance/aopalliance/1.0/aopalliance-1.0.jar";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        Path file = Files.createTempFile("testlib", ".jar");
        try(FileChannel fc = new FileOutputStream(file.toFile()).getChannel()) {
            fc.transferFrom(
                    Channels.newChannel(connection.getInputStream()),
                    0,
                    Long.MAX_VALUE
            );
        }

        return file;
    }
}
