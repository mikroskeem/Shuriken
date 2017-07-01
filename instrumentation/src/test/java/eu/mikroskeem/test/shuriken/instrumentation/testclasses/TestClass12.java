package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

/**
 * @author Mark Vainomaa
 */
public class TestClass12 {
    public TestClass12() {}
    private TestClass12(TestClass7 a) {}

    public TestClass12 a(TestClass7 a) {
        return new TestClass12(a);
    }
}
