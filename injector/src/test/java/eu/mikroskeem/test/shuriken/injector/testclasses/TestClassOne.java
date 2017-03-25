package eu.mikroskeem.test.shuriken.injector.testclasses;

import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Mark Vainomaa
 */
public class TestClassOne {
    @Getter @Inject @Named("name one") private Object a;
    @Getter @Inject @Named("name two") private String b;
}
