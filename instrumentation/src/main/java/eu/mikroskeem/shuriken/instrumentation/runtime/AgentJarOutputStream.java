package eu.mikroskeem.shuriken.instrumentation.runtime;

import eu.mikroskeem.shuriken.common.streams.ByteArrays;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static eu.mikroskeem.shuriken.instrumentation.ClassTools.getClassResourcePath;

/**
 * Agent jar output stream.
 *
 * @author Mark Vainomaa
 */
public class AgentJarOutputStream extends JarOutputStream {
    final String mainClassName;

    ZipEntry currentEntry = null;
    ByteArrayOutputStream currentEntryData = null;
    boolean mainClassAdded = false;

    @Override
    public void putNextEntry(ZipEntry zipEntry) throws IOException {
        currentEntry = zipEntry;
        currentEntryData = new ByteArrayOutputStream();
        super.putNextEntry(zipEntry);
    }

    @Override
    public void write(int b) throws IOException {
        currentEntryData.write(b);
        super.write(b);
    }

    @Override
    public void write(byte[] b, int len, int off) throws IOException {
        currentEntryData.write(b, len, off);
        super.write(b, len, off);
    }

    @Override
    public void write(byte[] b) throws IOException {
        currentEntryData.write(b);
        super.write(b);
    }

    @Override
    public void closeEntry() throws IOException {
        // Validate agent class
        if(currentEntry.getName().equals(mainClassName)) {
            AgentClassValidator.validateMainClass(this);
            mainClassAdded = true;
        }
        currentEntry = null;
        currentEntryData = null;
        super.closeEntry();
    }

    /**
     * Constructs new AgentJarOutputStream
     *
     * @param out Delegate output stream
     * @param man {@link Manifest} to supply with jar
     * @param mainClassName Main agent class name
     */
    public AgentJarOutputStream(@NotNull OutputStream out, @NotNull Manifest man, @NotNull String mainClassName) throws IOException {
        super(out);
        this.mainClassName = getClassResourcePath(mainClassName);

        // This is done in JarOutputStream constructor as well
        ByteArrayOutputStream manifest; man.write(manifest = new ByteArrayOutputStream());
        addEntry(JarFile.MANIFEST_NAME, manifest.toByteArray());
    }

    /**
     * Adds new entry into jar
     *
     * @param entryPath Entry path
     * @param data Entry data
     */
    public void addEntry(@NotNull String entryPath, @NotNull byte[] data) throws IOException {
        putNextEntry(new JarEntry(entryPath));
        write(data);
        closeEntry();
    }

    /**
     * Adds class to jar
     *
     * @param clazz Target {@link Class}
     */
    public void addClassToJar(@NotNull Class<?> clazz) throws IOException {
        String clz = getClassResourcePath(clazz);
        InputStream classStream;
        if((classStream = clazz.getClassLoader().getResourceAsStream(clz)) == null) {
            throw new IOException("Could not open resource: " + clz);
        }
        addEntry(clz, ByteArrays.fromInputStream(classStream));
    }

    /**
     * Merges other jar into this jar.
     *
     * @param otherJar Other jar
     */
    public void mergeJar(@NotNull ZipInputStream otherJar) throws IOException {
        ZipEntry entry;
        while((entry = otherJar.getNextEntry()) != null) {
            byte[] buf = new byte[(int)entry.getSize()];
            while(true)
                if(!(otherJar.read(buf) > 0))
                    break;
            addEntry(entry.getName(), buf);
        }
    }

    /**
     * Merges other jar into this jar.
     *
     * @param otherJar Other jar
     */
    public void mergeJar(@NotNull ZipFile otherJar) throws IOException {
        Enumeration<? extends ZipEntry> entries = otherJar.entries();
        ZipEntry entry;
        while(entries.hasMoreElements() && (entry = entries.nextElement()) != null) { // Last check isn't needed, I'm just lazy
            addEntry(entry.getName(), ByteArrays.fromInputStream(otherJar.getInputStream(entry)));
        }
    }

    /**
     * Merges other jar into this jar.
     *
     * @param otherJar Other jar
     */
    public void mergeJar(@NotNull Path otherJar) throws IOException {
        mergeJar(new ZipFile(otherJar.toFile()));
    }

    @Override
    public void finish() throws IOException {
        if(!mainClassAdded) {
            throw new IllegalStateException("Main agent class was not added into jar!");
        }
        super.finish();
    }
}
