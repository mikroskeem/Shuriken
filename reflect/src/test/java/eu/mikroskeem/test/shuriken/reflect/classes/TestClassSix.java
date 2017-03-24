package eu.mikroskeem.test.shuriken.reflect.classes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Mark Vainomaa
 */
public class TestClassSix {
    @Deprecated
    @Test1
    @Test2
    @Test3
    public String a = "a";

    @Retention(RetentionPolicy.RUNTIME) public @interface Test1 {}
    @Retention(RetentionPolicy.RUNTIME) public @interface Test2 {}
    @Retention(RetentionPolicy.RUNTIME) public @interface Test3 {}
}
