package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.FieldWrapper;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassFive;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassOne;
import eu.mikroskeem.test.shuriken.reflect.classes.TestClassTwo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper.of;

public class ClassWrapperTester {
    @Test
    public void testClassFinding() throws Exception {
        Class<?> testClass = TestClassOne.class;
        String className = testClass.getName();

        ClassWrapper<?> cw = Reflect.getClass(className).orElse(null);
        Assertions.assertNotNull(cw, "Class wrapper shouldn't be null!");
        Assertions.assertEquals(testClass, cw.getWrappedClass(), "Classes weren't equal!");
        Assertions.assertNull(cw.getClassInstance(), "Class instance shouldn't be set!");
    }

    @Test
    public void testClassConstructing() throws Exception {
        Class<TestClassOne> testClass = TestClassOne.class;
        ClassWrapper<TestClassOne> cw = Reflect.wrapClass(testClass).construct();
        Assertions.assertNotNull(cw.getClassInstance(), "Class instance shouldn't be null!");
    }

    @Test
    public void testClassConstructingWithArgs() throws Exception {
        Class<TestClassTwo> testClass = TestClassTwo.class;
        ClassWrapper<TestClassTwo> cw = Reflect.wrapClass(testClass)
                .construct(of("foo"));
        Assertions.assertNotNull(cw.getClassInstance(), "Class instance shouldn't be null!");
        TestClassTwo inst = cw.getClassInstance();
        Assertions.assertEquals("foo", inst.getA());
    }

    @Test
    public void testClassConstructingWithInvalidArgs() throws Exception {
        Class<TestClassTwo> testClass = TestClassTwo.class;
        ClassWrapper<TestClassTwo> cw = Reflect.wrapClass(testClass);
        NoSuchMethodException e = Assertions.assertThrows(NoSuchMethodException.class, ()->{
            Reflect.construct(cw, of(1));
        });
        Assertions.assertNull(cw.getClassInstance(), "Class instance should be null!");
    }

    @Test
    public void testInstanceWrapping() throws Exception {
        Class<TestClassOne> testClass = TestClassOne.class;
        TestClassOne testClassOne = new TestClassOne();
        ClassWrapper<TestClassOne> cw = Reflect.wrapInstance(testClassOne);
        Assertions.assertEquals(testClass, cw.getWrappedClass(), "Classes should match!");
    }

    @Test
    public void testInstanceReadingWithoutInstance() throws Exception {
        ClassWrapper<TestClassFive> testClass = Reflect.wrapClass(TestClassFive.class);
        FieldWrapper<String> field = testClass.getField("b", String.class).get();
        Assertions.assertThrows(IllegalAccessException.class, field::read);
        testClass.construct();
        Assertions.assertEquals("bar", field.read());
    }
}
