package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

import java.util.Properties;

/**
 * @author Mark Vainomaa
 */
public class TestTransformable5 {
    private String foundProperty;

    public void testProperty() {
        Properties properties = System.getProperties();
        foundProperty = properties.getProperty("java.home");
    }
}
