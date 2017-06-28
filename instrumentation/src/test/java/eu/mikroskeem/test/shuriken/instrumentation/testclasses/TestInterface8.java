package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

/**
 * @author Mark Vainomaa
 */
public interface TestInterface8 {
    default String a() {
        return "abcd";
    }

    String b();

    static String d() {
        return "foobar";
    }
}
