package eu.mikroskeem.test.shuriken.classloader;

import eu.mikroskeem.shuriken.classloader.ShurikenClassLoader;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
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
        String className = "eu.mikroskeem.test.shuriken.classloader.classes.TestClass1";
        File testJar = GenerateTestJar.generateJar(GenerateTestClass.generate());
        URL[] urls = new URL[]{testJar.toURI().toURL()};
        ShurikenClassLoader cl = new ShurikenClassLoader(urls, this.getClass().getClassLoader());

        /* Find class via reflection */
        Optional<ClassWrapper<?>> optClazz = Reflect.getClass(className, cl);
        Assertions.assertTrue(optClazz.isPresent(), "Class should be present!");

        /* Try constructing that class */
        ClassWrapper<?> clazz = optClazz.get().construct();
        Assertions.assertTrue(clazz.getClassInstance().getClass() == clazz.getWrappedClass());

        /* Peek into cached classes */
        ClassWrapper<ShurikenClassLoader> clw = Reflect.wrapInstance(cl);
        FieldWrapper<Map> classesField = clw.getField("uncompressedClasses", Map.class).get();
        Map<String, Class<?>> classes = (Map<String, Class<?>>) classesField.read();

        Assertions.assertTrue(new ArrayList<>(classes.keySet()).get(0).equals(className));
        Assertions.assertTrue(new ArrayList<>(classes.values()).get(0) == clazz.getWrappedClass());
    }
}
