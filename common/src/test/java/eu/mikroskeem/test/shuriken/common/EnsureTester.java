package eu.mikroskeem.test.shuriken.common;

import eu.mikroskeem.shuriken.common.Ensure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper.of;

public class EnsureTester {
    @Test
    public void testTrueCondition() throws Exception {
        Ensure.ensureCondition(true, UnsupportedOperationException.class, of("This shouldn't happen"));
    }

    @Test
    public void testFalseCondition() throws Exception {
        UnsupportedOperationException e = Assertions.assertThrows(UnsupportedOperationException.class, ()->{
            Ensure.ensureCondition(false, UnsupportedOperationException.class, of("foo"));
        });
        Assertions.assertEquals("foo", e.getMessage(), "Exception message didn't match!");
    }

    @Test
    public void testNull() throws Exception {
        String a = null;
        NullPointerException e = Assertions.assertThrows(NullPointerException.class, ()->{
            String _a = Ensure.notNull(a, "a is null");
        });
        Assertions.assertEquals("a is null", e.getMessage(), "Exception message didn't match!");
    }

    @Test
    public void testNotNull() throws Exception {
        String b = "woo";
        Ensure.notNull(b, "b shouldn't be null");
    }
}
