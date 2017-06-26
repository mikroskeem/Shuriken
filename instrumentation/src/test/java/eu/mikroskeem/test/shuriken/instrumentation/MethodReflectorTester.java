package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass3;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass4;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass5;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector.newInstance;
import static eu.mikroskeem.shuriken.reflect.Reflect.wrapClass;
import static eu.mikroskeem.shuriken.reflect.Reflect.wrapInstance;


/**
 * @author Mark Vainomaa
 */
public class MethodReflectorTester {
    @Test
    public void testAllPublicMethodReflector() {
        ClassWrapper<TestClass3> tc = wrapClass(TestClass3.class).construct();
        MethodReflector<TestClass3n4Reflector> reflector = newInstance(tc, TestClass3n4Reflector.class);

        TypeWrapper[] eParams = new TypeWrapper[] {
                TypeWrapper.of(int.class, 3),
                TypeWrapper.of("a"),
                TypeWrapper.of(char.class, 'a')
        };

        TestClass3n4Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals(tc.invokeMethod("a", String.class), reflectorImpl.a());
        Assertions.assertEquals(tc.invokeMethod("b", int.class).intValue(), reflectorImpl.b());
        Assertions.assertNull(tc.invokeMethod("c", void.class));
        Assertions.assertEquals(tc.invokeMethod("d", char.class).charValue(), reflectorImpl.d());
        Assertions.assertEquals(tc.invokeMethod("e", String.class, eParams), reflectorImpl.e(3, "a", 'a'));
    }

    @Test
    public void testMethodReflectorFactoryClassName() {
        String EXPECTED = "eu.mikroskeem.shuriken.instrumentation.methodreflector." +
                "Target$TestClass3$MethodReflectorTester$TestClass3n4Reflector$0";

        TestClass3 tc = new TestClass3();
        ClassWrapper<TestClass3> tcWrap = wrapInstance(tc);

        String MRF_CLASS = "eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflectorFactory";
        ClassWrapper<?> mrfClass = Reflect.getClass(MRF_CLASS).get();
        ClassWrapper<?> mrf = wrapInstance(wrapClass(MethodReflector.class).getField("factory", mrfClass).get().read());

        String className = mrf.invokeMethod("generateName",
                String.class,
                TypeWrapper.of(tcWrap),
                TypeWrapper.of(TestClass3n4Reflector.class));
        Assertions.assertEquals(EXPECTED, className, "Class names should equal");
    }

    @Test
    public void testMixedAccessMethodReflector() {
        ClassWrapper<TestClass4> tc = wrapClass(TestClass4.class).construct();
        MethodReflector<TestClass3n4Reflector> reflector = newInstance(tc, TestClass3n4Reflector.class);

        TypeWrapper[] eParams = new TypeWrapper[] {
                TypeWrapper.of(int.class, 3),
                TypeWrapper.of("a"),
                TypeWrapper.of(char.class, 'a')
        };

        TestClass3n4Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals(tc.invokeMethod("a", String.class), reflectorImpl.a());
        Assertions.assertEquals(tc.invokeMethod("b", int.class).intValue(), reflectorImpl.b());
        reflectorImpl.c();
        Assertions.assertEquals(tc.invokeMethod("d", char.class).charValue(), reflectorImpl.d());
        Assertions.assertEquals(tc.invokeMethod("e", String.class, eParams), reflectorImpl.e(3, "a", 'a'));
    }

    @Test
    public void testArrayMethodReflector() {
        ClassWrapper<TestClass5> tc = wrapClass(TestClass5.class).construct();
        MethodReflector<TestClass5Reflector> reflector = newInstance(tc, TestClass5Reflector.class);

        TestClass5Reflector reflectorImpl = reflector.getReflector();
        byte[] EXPECTED = new byte[] { 0, 1, 0, 1, 1 };
        Assertions.assertArrayEquals(EXPECTED, reflectorImpl.a());
    }

    public interface TestClass3n4Reflector {
        String a();
        int b();
        void c();
        char d();
        String e(int a, String b, char c);
    }

    public interface TestClass5Reflector {
        byte[] a();
    }
}
