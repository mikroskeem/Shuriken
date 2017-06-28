package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.methodreflector.*;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.*;
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
    public void testMethodReflector() {
        ClassWrapper<TestClass> tc = wrapClass(TestClass.class);
        MethodReflector<DummyInterface> reflector = newInstance(tc, DummyInterface.class);

        Assertions.assertEquals(DummyInterface.class, reflector.getInterface());
        Assertions.assertEquals(DummyInterface.class, reflector.getReflector().getClass().getInterfaces()[0]);
        Assertions.assertEquals(TestClass.class, reflector.getTargetClass().getWrappedClass());
    }

    @Test
    public void testMethodReflectorReUse() {
        ClassWrapper<TestClass> tc = wrapClass(TestClass.class);
        MethodReflector<DummyInterface> reflector1 = newInstance(tc, DummyInterface.class);
        MethodReflector<DummyInterface> reflector2 = newInstance(tc, DummyInterface.class);
        MethodReflector<DummyInterface> reflector3 = newInstance(tc, DummyInterface.class);
        MethodReflector<DummyInterface> reflector4 = newInstance(tc, DummyInterface.class);

        /* With reflector1 */
        Assertions.assertEquals(reflector1, reflector2);
        Assertions.assertEquals(reflector1, reflector3);
        Assertions.assertEquals(reflector1, reflector4);

        /* With reflector2 */
        Assertions.assertEquals(reflector2, reflector3);
        Assertions.assertEquals(reflector2, reflector4);

        /* With reflector3 */
        Assertions.assertEquals(reflector3, reflector4);
    }

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
        reflectorImpl.c();
        Assertions.assertEquals(tc.invokeMethod("d", char.class).charValue(), reflectorImpl.d());
        Assertions.assertEquals(tc.invokeMethod("e", String.class, eParams), reflectorImpl.e(3, "a", 'a'));
        Assertions.assertEquals(tc.invokeMethod("e", String.class, eParams), reflectorImpl.altE(3, "a", 'a'));
    }

    @Test
    public void testMethodReflectorFactoryClassName() {
        String EXPECTED = "eu.mikroskeem.shuriken.instrumentation.methodreflector." +
                "Target$TestClass3$MethodReflectorTester$DummyInterface2$0";

        TestClass3 tc = new TestClass3();
        ClassWrapper<TestClass3> tcWrap = wrapInstance(tc);

        String MRF_CLASS = "eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflectorFactory";
        ClassWrapper<?> mrfClass = Reflect.getClass(MRF_CLASS).get();
        ClassWrapper<?> mrf = wrapInstance(wrapClass(MethodReflector.class).getField("factory", mrfClass).get().read());

        String className = mrf.invokeMethod("generateName",
                String.class,
                TypeWrapper.of(tcWrap),
                TypeWrapper.of(DummyInterface2.class));
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

    @Test
    public void testInterfaceDefaultReflection() {
        ClassWrapper<TestClass8> tc = wrapClass(TestClass8.class).construct();
        MethodReflector<TestClass8Reflector> reflector = newInstance(tc, TestClass8Reflector.class);

        TestClass8Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals("abcd", reflectorImpl.a());
        Assertions.assertEquals("1234", reflectorImpl.b());
    }

    @Test
    public void testReflectiveInterfaceDefaultReflection() {
        ClassWrapper<TestClass8> tc = wrapClass(TestClass8.class).construct();
        MethodReflector<TestClass8ReflectorWithDefault> reflector = newInstance(tc, TestClass8ReflectorWithDefault.class);

        TestClass8ReflectorWithDefault reflectorImpl = reflector.getReflector();
        Assertions.assertNotEquals("cdef", reflectorImpl.a(),
                "Reflector interface default shouldn't be used!");
        Assertions.assertEquals("abcd", reflectorImpl.a());
        Assertions.assertEquals("1234", reflectorImpl.b());
        Assertions.assertEquals("fghi", reflectorImpl.c());
    }

    @Test
    public void testInterfaceStaticReflection() {
        ClassWrapper<TestClass8> tc = wrapClass(TestClass8.class).construct();
        MethodReflector<TestClass8StaticReflector> reflector = newInstance(tc, TestClass8StaticReflector.class);

        TestClass8StaticReflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals("foobar", reflectorImpl.d());
    }

    @Test
    public void testFieldGetterMethodReflector() {
        ClassWrapper<TestClass6> tc = wrapClass(TestClass6.class).construct();
        MethodReflector<TestClass6Reflector> reflector = newInstance(tc, TestClass6Reflector.class);

        TestClass6Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals("abcdef", reflectorImpl.getA());
        Assertions.assertEquals("c", reflectorImpl.getB());
        Assertions.assertEquals("d", reflectorImpl.getFinalC());
    }

    @Test
    public void testFieldSetterMethodReflector() {
        ClassWrapper<TestClass6> tc = wrapClass(TestClass6.class).construct();
        MethodReflector<TestClass6Reflector> reflector = newInstance(tc, TestClass6Reflector.class);

        TestClass6Reflector reflectorImpl = reflector.getReflector();
        reflectorImpl.setB("d");
        Assertions.assertEquals("d", reflectorImpl.getB());

        reflectorImpl.setD("j");
        Assertions.assertEquals("j", reflectorImpl.getD());
    }

    @Test
    public void testFinalFieldSetterMethodReflector() {
        ClassWrapper<TestClass6> tc = wrapClass(TestClass6.class).construct();
        MethodReflector<TestClass6Reflector> reflector = newInstance(tc, TestClass6Reflector.class);

        TestClass6Reflector reflectorImpl = reflector.getReflector();

        reflectorImpl.setFinalC("e");
        Assertions.assertEquals("e", reflectorImpl.getFinalC());

        reflectorImpl.setFinalF("i");
        Assertions.assertEquals("i", reflectorImpl.getFinalF());
    }

    @Test
    public void testClassConstruction() {
        ClassWrapper<TestClass7> tc = wrapClass(TestClass7.class);
        MethodReflector<TestClass7Reflector> reflector = newInstance(tc, TestClass7Reflector.class);

        TestClass7Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals(TestClass7.class, reflectorImpl.New().getClass());
        Assertions.assertEquals(TestClass7.class, reflectorImpl.New("a", "b", "c").getClass());
        Assertions.assertEquals(TestClass7.class, reflectorImpl.New("a").getClass());
        Assertions.assertEquals(TestClass7.class, reflectorImpl.New('a', -1).getClass());
    }

    public interface DummyInterface {}
    public interface DummyInterface2 {}

    public interface TestClass3n4Reflector {
        String a();
        int b();
        void c();
        char d();
        String e(int a, String b, char c);

        @TargetMethod("e") String altE(int a, String b, char c);
    }

    public interface TestClass5Reflector {
        byte[] a();
    }

    public interface TestClass6Reflector {
        @TargetFieldGetter("a") String getA();

        @TargetFieldGetter("b") String getB();
        @TargetFieldSetter("b") void setB(String b);

        @TargetFieldGetter("c") String getFinalC();
        @TargetFieldSetter("c") void setFinalC(String c);

        @TargetFieldGetter("d") String getD();
        @TargetFieldSetter("d") void setD(String d);

        @TargetFieldGetter("f") String getFinalF();
        @TargetFieldSetter("f") void setFinalF(String F);
    }

    public interface TestClass7Reflector {
        @TargetConstructor TestClass7 New();
        @TargetConstructor TestClass7 New(String a, String b, String c);
        @TargetConstructor TestClass7 New(String a);
        @TargetConstructor TestClass7 New(char c, int i);
    }

    public interface TestClass8Reflector {
        String a();
        String b();
    }

    public interface TestClass8ReflectorWithDefault {
        default String a() {
            return "cdef";
        }
        String b();
        default String c() {
            return "fghi";
        }
    }

    public interface TestClass8StaticReflector {
        String d();
    }
}
