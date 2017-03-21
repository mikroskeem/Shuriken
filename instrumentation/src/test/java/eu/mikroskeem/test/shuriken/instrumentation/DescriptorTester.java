package eu.mikroskeem.test.shuriken.instrumentation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static eu.mikroskeem.shuriken.instrumentation.Descriptor.newDescriptor;

public class DescriptorTester {
    @Test
    public void testDescriptorGenerator(){
        String desc = newDescriptor()
                .returns(void.class)
                .toString();
        String desc2 = newDescriptor()
                .returns(Void.class)
                .toString();
        String desc3 = newDescriptor()
                .accepts(int.class, int.class)
                .returns(String.class)
                .toString();
        Assertions.assertEquals("()V", desc);
        Assertions.assertEquals("()Ljava/lang/Void;", desc2); // huh?
        Assertions.assertEquals("(II)Ljava/lang/String;", desc3);
    }
}
