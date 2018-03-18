package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.Descriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DescriptorTester {
    @Test
    public void testDescriptorGenerator(){
        String desc = new Descriptor()
                .returns(void.class)
                .toString();
        String desc2 = new Descriptor()
                .returns(Void.class)
                .toString();
        String desc3 = new Descriptor()
                .accepts(int.class, int.class)
                .returns(String.class)
                .toString();
        Assertions.assertEquals("()V", desc);
        Assertions.assertEquals("()Ljava/lang/Void;", desc2); // huh?
        Assertions.assertEquals("(II)Ljava/lang/String;", desc3);
    }
}
