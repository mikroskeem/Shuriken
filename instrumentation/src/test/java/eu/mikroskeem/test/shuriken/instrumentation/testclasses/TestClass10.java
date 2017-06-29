package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

/**
 * @author Mark Vainomaa
 */
class TestClass10 {
    private String a = "a";
    private TestClass11 tc11 = new TestClass11();

    private TestClass11 b() {
        return tc11;
    }
}
