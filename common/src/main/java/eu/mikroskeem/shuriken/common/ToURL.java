package eu.mikroskeem.shuriken.common;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Convert various objects into URL without checked {@link java.net.MalformedURLException} exception
 *
 * @author Mark Vainomaa
 */
public class ToURL {
    /** Convert {@link Path} to {@link URL} */
    @NotNull @SneakyThrows(MalformedURLException.class)
    public static URL to(Path path) {
        return path.toUri().toURL();
    }

    /** Convert {@link File} to {@link URL} */
    @NotNull @SneakyThrows(MalformedURLException.class)
    public static URL to(File file) {
        return file.toURI().toURL();
    }
}
