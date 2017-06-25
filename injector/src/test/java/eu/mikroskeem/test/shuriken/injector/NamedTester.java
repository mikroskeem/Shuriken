package eu.mikroskeem.test.shuriken.injector;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.FieldWrapper;
import eu.mikroskeem.test.shuriken.injector.testclasses.TestClassThree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Named;
import java.lang.annotation.Annotation;

/**
 * @author Mark Vainomaa
 */
public class NamedTester {
    @Test
    public void testNamed() throws Exception {
        eu.mikroskeem.shuriken.injector.Named named = eu.mikroskeem.shuriken.injector.Named.as("a");

        ClassWrapper<TestClassThree> testClass = Reflect.wrapClass(TestClassThree.class).construct();
        FieldWrapper<String> field = testClass.getField("a", String.class).get();
        Named fieldNamed = field.getAnnotation(Named.class).get();

        Assertions.assertTrue(annotationEquals(named, fieldNamed));
    }

    private boolean annotationEquals(Annotation a, Annotation b){
        return a.equals(b);
    }
}
