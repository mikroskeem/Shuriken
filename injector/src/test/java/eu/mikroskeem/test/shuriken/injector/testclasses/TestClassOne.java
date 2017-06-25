package eu.mikroskeem.test.shuriken.injector.testclasses;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Mark Vainomaa
 */
public class TestClassOne {
    @Inject @Named("name one") private Object a;
    @Inject @Named("name two") private String b;

    public Object getA() {
        return a;
    }

    public String getB() {
        return b;
    }
}
