package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.utils.FunctionalField;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassSeven;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Vainomaa
 */
public class FunctionalTest {
    @Test
    public void testFunctionalOne() {
        ClassWrapper<TestClassSeven> testClass = Reflect.wrapClass(TestClassSeven.class).construct();
        testClass.getFields().stream().filter(FunctionalField::notNullValue).forEach(FunctionalField::writeNull);
        Assertions.assertEquals(0, testClass.getFields().stream().filter(FunctionalField::notNullValue).count());
    }
}
