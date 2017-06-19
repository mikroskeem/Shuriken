package eu.mikroskeem.shuriken.common;

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
    @NotNull
    public static URL to(Path path) {
        try {
            return Ensure.notNull(path, "Path shouldn't be null!").toUri().toURL();
        }
        catch (MalformedURLException e) { SneakyThrow.throwException(e); }
        return null;
    }

    /** Convert {@link File} to {@link URL} */
    @NotNull
    public static URL to(File file) {
        try {
            return Ensure.notNull(file, "File shouldn't be null!").toURI().toURL();
        }
        catch (MalformedURLException e) { SneakyThrow.throwException(e); }
        return null;
    }
}
