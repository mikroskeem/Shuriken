package eu.mikroskeem.test.shuriken.classloader;

import eu.mikroskeem.shuriken.classloader.ShurikenClassLoader;
import eu.mikroskeem.shuriken.common.ToURL;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.FieldWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ShurikenClassLoader} unit tests
 *
 * @author Mark Vainomaa
 */
public class TestClassLoader {
    @Test
    @SuppressWarnings("unchecked")
    public void testClassLoader() throws Exception {
        BrotliLibraryLoader.loadBrotli();
        String className = "eu.mikroskeem.test.shuriken.classloader.classes.TestClass1";
        Path testJar = Utils.generateTestJar(GenerateTestClass.generate());
        URL[] urls = new URL[]{ToURL.to(testJar)};
        ShurikenClassLoader cl = new ShurikenClassLoader(urls, this.getClass().getClassLoader());

        /* Find class via reflection */
        Optional<ClassWrapper<?>> optClazz = Reflect.getClass(className, cl);
        Assertions.assertTrue(optClazz.isPresent(), "Class should be present!");

        /* Try constructing that class */
        ClassWrapper<?> clazz = optClazz.get().construct();
        Assertions.assertTrue(clazz.getClassInstance().getClass() == clazz.getWrappedClass());

        /* Peek into cached classes */
        ClassWrapper<ShurikenClassLoader> clw = Reflect.wrapInstance(cl);
        FieldWrapper<Map> classesField = clw.getField("classes", Map.class).get();
        Map<String, Class<?>> classes = (Map<String, Class<?>>) classesField.read();

        Assertions.assertTrue(new ArrayList<>(classes.keySet()).get(0).equals(className));
        Assertions.assertTrue(new ArrayList<>(classes.values()).get(0) == clazz.getWrappedClass());
    }
}
