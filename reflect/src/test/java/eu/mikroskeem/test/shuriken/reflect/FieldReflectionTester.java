package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.FieldWrapper;
import eu.mikroskeem.test.shuriken.reflect.classes.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
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

    @Test
    public void testFinalFieldWriting() throws Exception {
        ClassWrapper<TestClassFive> cw = Reflect.wrapClass(TestClassFive.class).construct();

        Optional<FieldWrapper<String>> fieldOpt = cw.getField("a", String.class);
        Assertions.assertTrue(fieldOpt.isPresent(), "Field should be present!");

        fieldOpt.get().write("kek");
        Assertions.assertEquals("kek", fieldOpt.get().read());
    }

    @Test
    public void testIsFieldStatic() throws Exception {
        ClassWrapper<TestClassFour> cw = Reflect.wrapClass(TestClassFour.class);
        boolean isStatic = cw.getField("a", double[].class).get().isStatic();
        Assertions.assertTrue(isStatic);
    }

    @Test
    public void testFieldListing() throws Exception {
        ClassWrapper<TestClassFive> cw = Reflect.wrapClass(TestClassFive.class);
        List<FieldWrapper<?>> fields = cw.getFields();
        Assertions.assertTrue(fields.size() == 2);
        Assertions.assertTrue(fields.get(0).getType() == String.class);
        Assertions.assertTrue(fields.get(1).getType() == String.class);
    }

    @Test
    public void testAnnotationGetting() throws Exception {
        ClassWrapper<TestClassSix> cw = Reflect.wrapClass(TestClassSix.class);
        FieldWrapper<String> field = cw.getField("a", String.class).get();
        Assertions.assertTrue(field.getAnnotation(Deprecated.class).isPresent());
    }

    @Test
    public void testAnnotationListing() throws Exception {
        ClassWrapper<TestClassSix> cw = Reflect.wrapClass(TestClassSix.class);
        FieldWrapper<String> field = cw.getField("a", String.class).get();
        List<? extends Annotation> annotations = field.getAnnotations();
        /*
         * Comparing annotation classes directly doesn't seem to be a good idea, because
         * you'll get stuff like this, if you print them out:
         *     class com.sun.proxy.$Proxy9
         *     class com.sun.proxy.$Proxy10
         * Well TIL I guess
         */
        Assertions.assertTrue(Deprecated.class.isAssignableFrom(annotations.get(0).getClass()));
        Assertions.assertTrue(TestClassSix.Test1.class.isAssignableFrom(annotations.get(1).getClass()));
        Assertions.assertTrue(TestClassSix.Test2.class.isAssignableFrom(annotations.get(2).getClass()));
        Assertions.assertTrue(TestClassSix.Test3.class.isAssignableFrom(annotations.get(3).getClass()));
    }

    @Test
    public void testUnknownObjectReflection() throws Exception {
        ClassWrapper<TestClassFive> cw = Reflect.wrapClass(TestClassFive.class).construct();
        FieldWrapper<Object> field = cw.getField("a", Object.class).get();

        Assertions.assertEquals(Object.class, field.getType());
        Assertions.assertEquals("foo", field.read());
    }
}
