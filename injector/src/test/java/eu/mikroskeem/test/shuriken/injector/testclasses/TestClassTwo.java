package eu.mikroskeem.test.shuriken.injector.testclasses;

import javax.inject.Inject;

/**
 * @author Mark Vainomaa
 */
public class TestClassTwo {
    @Inject private InterfacesTestClass.a a;
    @Inject private InterfacesTestClass.b b;

    public InterfacesTestClass.a getA() {
        return a;
    }

    public InterfacesTestClass.b getB() {
        return b;
    }
}
