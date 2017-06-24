package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassFour;
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

    @Test
    public void testClassArrayMethodInvoke() throws Exception {
        ClassWrapper<TestClassFour> cw = Reflect.wrapClass(TestClassFour.class);
        Double[] expected = new Double[]{-1D, -1D, -1D};
        Double[] actual = cw.invokeMethod("get2", Double[].class);
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    public void testClassPrimitiveArrayMethodInvoke() throws Exception {
        ClassWrapper<TestClassFour> cw = Reflect.wrapClass(TestClassFour.class);
        double[] expected = new double[]{-1, -1, -1};
        double[] actual = cw.invokeMethod("get", double[].class);
        Assertions.assertArrayEquals(expected, actual);
    }
}
