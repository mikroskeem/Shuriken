package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

import java.io.PrintStream;
import java.util.Properties;

/**
 * @author Mark Vainomaa
 */
public class TestTransformable4 {
    private static PrintStream dummyStream = new PrintStream(System.out) {
        @Override
        public void println(String text) {
            super.println("[Retransformed] " + text);
        }
    };

    public void testProperty() {
        Properties properties = System.getProperties();
        if(properties.getProperty("shuriken.testagent4").equals("true")) {
            System.out.println("Method reroute failed");
        }
    }

    public void testPrint() {
        System.out.println("Test");
    }

    // System.getProperties() gets replaced with this
    public static Properties getDummyProperties() {
        Properties props = new Properties();
        props.setProperty("shuriken.testagent4", "false");
        return props;
    }

    // GETSTATIC System.out gets replaced with this
    public static PrintStream getDummyStream() {
        return dummyStream;
    }
}
