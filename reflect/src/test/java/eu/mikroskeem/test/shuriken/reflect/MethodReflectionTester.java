package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassOne;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MethodReflectionTester {
    @Test
    public void testClassMethodInvoke() throws Exception {
        Class<?> testClass = TestClassOne.class;
        ClassWrapper<?> cw = Reflect.wrapClass(testClass);
        Reflect.construct(cw);

        /* Instance method */
        String a = cw.invokeMethod("a", String.class);
        Assertions.assertEquals("a", a);

        /* Static method */
        String b = cw.invokeMethod("b", String.class);
        Assertions.assertEquals("b", b);

        /* Void method */
        Assertions.assertNull(cw.invokeMethod("c", Void.class));
        Assertions.assertNull(cw.invokeMethod("c", void.class));
    }

    @Test
    public void testClassPrimitiveMethodInvoke() throws Exception {
        Class<?> testClass = TestClassOne.class;
        ClassWrapper<?> cw = Reflect.wrapClass(testClass).construct();

        double expected = 'a';
        double actual = cw.invokeMethod("d", char.class);
        Assertions.assertEquals(expected, actual);
    }
}
