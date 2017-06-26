package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

/**
 * @author Mark Vainomaa
 */
public class TestClass4 {
    private String a() { return ""; };
    int b() { return 0; }
    public void c() {}
    char d() { return 'a'; }
    private static String e(int a, String b, char c) { return TestClass3.a(); }
}
