package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetConstructor;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetFieldGetter;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetFieldSetter;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.TargetMethod;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass12;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass3;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass4;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass5;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass6;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass7;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass8;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass9;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector.newInstance;
import static eu.mikroskeem.shuriken.reflect.Reflect.wrapClass;
import static eu.mikroskeem.shuriken.reflect.Reflect.wrapInstance;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;


/**
 * @author Mark Vainomaa
 */
/* Use PER_METHOD, otherwise this test blows up */
@TestInstance(PER_METHOD)
public class MethodReflectorTester {
    private final static String TC7Type = "Leu/mikroskeem/test/shuriken/instrumentation/testclasses/TestClass7;";
    private final static String TC11Type = "Leu/mikroskeem/test/shuriken/instrumentation/testclasses/TestClass11;";
    private final static String TC12Type = "Leu/mikroskeem/test/shuriken/instrumentation/testclasses/TestClass12;";

    @BeforeAll
    public static void setupMethodReflector() {
        MethodReflector.DEBUG = true;
    }

    @AfterAll
    public static void cleanMethodReflector() {
        MethodReflector.DEBUG = false;
    }

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
    @Disabled("Unsupported")
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

        /* TODO: Temporary workaround, before I fix unit tests properly. */
        reflectorImpl.setFinalC("d");
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

    @Test
    public void testGenericsReflection() {
        ClassWrapper<TestClass9> tc = wrapClass(TestClass9.class).construct();
        MethodReflector<TestClass9Reflector> reflector = newInstance(tc, TestClass9Reflector.class);

        TestClass9Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals("a", reflectorImpl.getA().get(0));

        /*
         * Note: Fucking type erasure
         * https://en.wikipedia.org/wiki/Generics_in_Java#Problems_with_type_erasure
         */
        //noinspection AssertEqualsBetweenInconvertibleTypes
        Assertions.assertEquals("a", reflectorImpl.getAFaulty().get(0));
    }

    @Test
    public void testReflectUsingObjects() {
        ClassWrapper<TestClass4> tc = wrapClass(TestClass4.class).construct();
        MethodReflector<TestClass4ObjectReflector> reflector = newInstance(tc, TestClass4ObjectReflector.class);

        TestClass4ObjectReflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals(String.class, reflectorImpl.a().getClass());
        Assertions.assertEquals(Integer.class, reflectorImpl.b().getClass());
        Assertions.assertEquals(String.class, reflectorImpl.e(1, "a", 'a').getClass());
        Assertions.assertEquals(String.class, reflectorImpl.altE(1, "a", 'a').getClass());
    }

    @Test
    public void testReflectionOnNonPublicClass() {
        ClassWrapper<?> tc = Reflect.getClass(TestClass9.class.getPackage().getName() + ".TestClass10").orElse(null);
        tc.construct();
        MethodReflector<TestClass10Reflector> reflector = newInstance(tc, TestClass10Reflector.class);

        TestClass10Reflector reflectorImpl = reflector.getReflector();
        Assertions.assertEquals("a", reflectorImpl.getA());
        reflectorImpl.setA("b");
        Assertions.assertEquals("b", reflectorImpl.getA());

        Object tc11ref = reflectorImpl.getTC11();

        Assertions.assertNotNull(tc11ref);
        Assertions.assertEquals(tc11ref, reflectorImpl.getTC11Second());

        /* Replace TestClass11 instance */
        Object newRef = Reflect.wrapClass(tc11ref.getClass()).construct().getClassInstance();
        reflectorImpl.setTC11(newRef);
        Assertions.assertEquals(newRef, reflectorImpl.getTC11Second());
    }

    @Test
    public void testReflectiveConstructionOnNonPublicClass() {
        ClassWrapper<?> tc = Reflect.getClass(TestClass9.class.getPackage().getName() + ".TestClass11").orElse(null);
        MethodReflector<TestClass11Reflector> reflector = newInstance(tc, TestClass11Reflector.class);

        TestClass11Reflector reflectorImpl = reflector.getReflector();
        Object tc11Ref = reflectorImpl.New();
        Assertions.assertNotNull(tc11Ref);
        Assertions.assertEquals(tc.getWrappedClass(), tc11Ref.getClass());
    }

    @Test
    public void testReflectingWithPlaceholders() {
        ClassWrapper<TestClass12> tc = Reflect.wrapClass(TestClass12.class);
        ClassWrapper<TestClass7> tc7 = Reflect.wrapClass(TestClass7.class);
        tc.construct();
        tc7.construct();
        MethodReflector.registerAnnotationReplacement("tc7", TC7Type);
        MethodReflector.registerAnnotationReplacement("tc12", TC12Type);
        MethodReflector<TestClass12PlaceholderReflector> reflector = newInstance(tc, TestClass12PlaceholderReflector.class);

        TestClass12PlaceholderReflector reflectorImpl = reflector.getReflector();


        Object tc12Ref = reflectorImpl.New();
        Object tc12Ref2 = reflectorImpl.a(tc7.getClassInstance());
        Object tc12Ref3 = reflectorImpl.New(tc7.getClassInstance());

        Assertions.assertNotNull(tc12Ref);
        Assertions.assertNotNull(tc12Ref2);

        Assertions.assertEquals(tc.getWrappedClass(), tc12Ref.getClass());
        Assertions.assertEquals(tc.getWrappedClass(), tc12Ref2.getClass());
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

    public interface TestClass4ObjectReflector {
        @TargetMethod(desc = "()Ljava/lang/String;") Object a();
        @TargetMethod(desc = "()I") Object b();

        /* All objects */
        @TargetMethod(desc = "(ILjava/lang/String;C)Ljava/lang/String;")
        Object e(Object a, Object b, Object c);

        /* Only some objects */
        @TargetMethod(value = "e", desc = "(ILjava/lang/String;C)Ljava/lang/String;")
        String altE(int a, String b, Object c);
    }

    public interface TestClass9Reflector {
        @TargetFieldGetter("a") List<String> getA();
        @TargetFieldGetter("a") List<Integer> getAFaulty();
    }

    public interface TestClass10Reflector {
        @TargetFieldGetter("a") String getA();
        @TargetFieldSetter("a") void setA(String a);
        @TargetFieldGetter(value = "tc11", type = TC11Type) Object getTC11();
        @TargetFieldSetter(value = "tc11", type = TC11Type) void setTC11(Object a);
        @TargetMethod(value = "b", desc = "()"+TC11Type) Object getTC11Second();
    }

    public interface TestClass11Reflector {
        @TargetConstructor(desc = "()" + TC11Type) Object New();
    }

    public interface TestClass12PlaceholderReflector {
        /* Note: target return type isn't checked on constructor invokers */
        @TargetConstructor(desc = "(){tc12}") Object New();
        @TargetConstructor(desc = "({tc7}){tc12}") Object New(Object tc7);

        @TargetMethod(desc = "({tc7}){tc12}") TestClass12 a(Object a);
    }
}
