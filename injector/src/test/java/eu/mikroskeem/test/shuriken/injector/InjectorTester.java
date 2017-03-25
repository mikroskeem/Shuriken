package eu.mikroskeem.test.shuriken.injector;

import eu.mikroskeem.shuriken.injector.Injector;
import eu.mikroskeem.shuriken.injector.ShurikenInjector;
import eu.mikroskeem.test.shuriken.injector.testclasses.InterfacesTestClass;
import eu.mikroskeem.test.shuriken.injector.testclasses.TestClassOne;
import eu.mikroskeem.test.shuriken.injector.testclasses.TestClassTwo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * @author Mark Vainomaa
 */
public class InjectorTester {
    @Test
    public void testBindingInjecting() throws Exception {
        Injector injector = ShurikenInjector.createInjector(binder -> {
           binder.bind(InterfacesTestClass.a.class).to(InterfacesTestClass.A.class);
           binder.bind(InterfacesTestClass.b.class).to(InterfacesTestClass.C.class);
        });

        TestClassTwo t1 = injector.getInstance(TestClassTwo.class);
        TestClassTwo t2 = injector.getInstance(TestClassTwo.class);

        Assertions.assertEquals("a implementer A", t1.getA().toString());
        Assertions.assertEquals("b implementer C", t1.getB().toString());
        Assertions.assertEquals("a implementer A", t2.getA().toString());
        Assertions.assertEquals("b implementer C", t2.getB().toString());
        Assertions.assertNotEquals(t1.getA(), t2.getA());
        Assertions.assertNotEquals(t1.getB(), t2.getB());
    }

    @Test
    public void testSingletonInjecting() throws Exception {
        Injector injector = ShurikenInjector.createInjector(binder -> {
            binder.bind(InterfacesTestClass.a.class).toSingleton(InterfacesTestClass.B.class);
            binder.bind(InterfacesTestClass.b.class).toSingleton(InterfacesTestClass.D.class);
        });

        TestClassTwo t1 = injector.getInstance(TestClassTwo.class);
        TestClassTwo t2 = injector.getInstance(TestClassTwo.class);

        Assertions.assertEquals("a implementer B", t1.getA().toString());
        Assertions.assertEquals("b implementer D", t1.getB().toString());
        Assertions.assertEquals("a implementer B", t2.getA().toString());
        Assertions.assertEquals("b implementer D", t2.getB().toString());
        Assertions.assertEquals(t1.getA(), t2.getA());
        Assertions.assertEquals(t1.getB(), t2.getB());
    }

    @Test
    public void testInstanceInjector() throws Exception {
        Object obj1 = new Object();
        String obj2 = "yaay";

        Injector injector = ShurikenInjector.createInjector(binder -> {
            binder.bind(Object.class)
                    //.annotatedWith(Named.as("name one")) TODO
                    .toInstance(obj1);
            binder.bind(String.class)
                    //.annotatedWith(Named.as("name two")) TODO
                    .toInstance(obj2);
        });

        TestClassOne testClass = injector.getInstance(TestClassOne.class);
        Assertions.assertEquals(obj1, testClass.getA(), "Field a should not be null!");
        Assertions.assertEquals(obj2, testClass.getB(), "Field b should not be null!");
    }

    @Test
    @Disabled("javax.inject.Named isn't supported yet")
    public void testNamedAnnotations() throws Exception {
        /*
        Injector injector = ShurikenInjector.createInjector(binder -> {
            binder.bind(Object.class)
                    .annotatedWith(Named.as("name one"))
                    .toInstance("foo");
            binder.bind(String.class)
                    .annotatedWith(Named.as("name two"))
                    .toInstance("bar");
        });
        */
    }
}
