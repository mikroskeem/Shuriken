package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassFour;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassOne;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassThree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class FieldReflectionTester {
    @Test
    public void testClassFieldReading() throws Exception {
        Class<?> testClass = TestClassOne.class;
        ClassWrapper<?> cw = Reflect.wrapClass(testClass);
        Reflect.construct(cw);

        Optional<FieldWrapper<String>> stringFieldOptional = cw.getField("kek", String.class);
        Assertions.assertTrue(stringFieldOptional.isPresent(), "String field shouldn't be null!");
        Assertions.assertEquals("foo", stringFieldOptional.get().read(), "Field value didn't match!");
    }

    @Test
    public void testClassFieldWriting() throws Exception {
        Class<?> testClass = TestClassOne.class;
        ClassWrapper<?> cw = Reflect.wrapClass(testClass);
        Reflect.construct(cw);

        Optional<FieldWrapper<String>> stringFieldOptional = cw.getField("kek", String.class);
        Assertions.assertTrue(stringFieldOptional.isPresent(), "String field shouldn't be null!");
        String oldValue = stringFieldOptional.get().read();
        String newValue = "bar";
        Assertions.assertEquals("foo", oldValue, "Field value didn't match!");
        stringFieldOptional.get().write(newValue);
        Assertions.assertEquals(newValue, stringFieldOptional.get().read(), "Field value didn't match!");
        stringFieldOptional.get().write(oldValue);
    }

    @Test
    public void testClassPrimitiveFieldReading() throws Exception {
        Class<?> testClass = TestClassOne.class;
        ClassWrapper<?> cw = Reflect.wrapClass(testClass);
        Reflect.construct(cw);

        Optional<FieldWrapper<Character>> fieldOptinal = cw.getField("a", char.class);
        Assertions.assertTrue(fieldOptinal.isPresent());
        FieldWrapper<Character> field = fieldOptinal.get();
        char expected = 'a';
        char actual = field.read();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testClassPrimitiveFieldWriting() throws Exception {
        Class<?> testClass = TestClassThree.class;
        ClassWrapper<?> cw = Reflect.wrapClass(testClass);
        Reflect.construct(cw);

        Optional<FieldWrapper<Character>> fieldOptinal = cw.getField("b", char.class);
        Assertions.assertTrue(fieldOptinal.isPresent());
        FieldWrapper<Character> field = fieldOptinal.get();

        char expected = 'f';
        field.write(expected);
        char actual = field.read();
        Assertions.assertEquals(expected, actual);
    }

    /*
     TODO: proper primitive array support?
     */

    @Test
    public <C> void testClassPrimitiveArrayFieldReading() throws Exception {
        ClassWrapper<TestClassFour> cw = Reflect.wrapClass(TestClassFour.class);

        Class<C> type = (Class<C>)double[].class;
        Optional<FieldWrapper<C>> fieldOptinal = cw.getField("a", type);
        Assertions.assertTrue(fieldOptinal.isPresent());
        FieldWrapper<C> field = fieldOptinal.get();
        double[] actual = (double[]) field.read();
        Assertions.assertEquals(-2D, actual[0]);
        Assertions.assertEquals(-2D, actual[1]);
        Assertions.assertEquals(-2D, actual[2]);
    }

    @Test
    public <C> void testClassPrimitiveArrayFieldWriting() throws Exception {
        ClassWrapper<TestClassFour> cw = Reflect.wrapClass(TestClassFour.class);

        Class<C> type = (Class<C>)double[].class;
        Optional<FieldWrapper<C>> fieldOptinal = cw.getField("a", type);
        Assertions.assertTrue(fieldOptinal.isPresent());
        FieldWrapper<C> field = fieldOptinal.get();
        double[] expected = new double[]{-2, -2, -2};
        field.write((C)expected);
        double[] actual = (double[]) field.read();
        Assertions.assertEquals(-2D, actual[0]);
        Assertions.assertEquals(-2D, actual[1]);
        Assertions.assertEquals(-2D, actual[2]);
    }
}
