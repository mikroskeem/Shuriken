package eu.mikroskeem.test.shuriken.injector.testclasses;

import lombok.Getter;

import javax.inject.Inject;

/**
 * @author Mark Vainomaa
 */
public class TestClassTwo {
    @Inject @Getter private InterfacesTestClass.a a;
    @Inject @Getter private InterfacesTestClass.b b;
}
