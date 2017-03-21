package eu.mikroskeem.test.shuriken.reflect;

import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CallerSensitiveTest {
    @Test
    public void testCallerSensitive() throws Exception {
        ClassWrapper<String> s = Reflect.wrapClass(String.class);
        Assertions.assertThrows(IllegalAccessError.class, ()->{
            Reflect.QuietReflect.THE_QUIET.construct(s).getClassInstance();
        });
    }
}
