package eu.mikroskeem.test.shuriken.injector.testclasses;

/**
 * @author Mark Vainomaa
 */
public class InterfacesTestClass {
    public interface a {}
    public interface b {}

    public static class A implements a {
        @Override
        public String toString() {
            return "a implementer A";
        }
    }
    public static class B implements a {
        @Override
        public String toString() {
            return "a implementer B";
        }
    }
    public static class C implements b {
        @Override
        public String toString() {
            return "b implementer C";
        }
    }
    public static class D implements b {
        @Override
        public String toString() {
            return "b implementer D";
        }
    }
}
