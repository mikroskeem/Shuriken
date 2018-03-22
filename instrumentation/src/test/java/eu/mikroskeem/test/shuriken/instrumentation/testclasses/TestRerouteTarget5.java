package eu.mikroskeem.test.shuriken.instrumentation.testclasses;

import java.util.Properties;

/**
 * @author Mark Vainomaa
 */
public final class TestRerouteTarget5 {
    public static String rerouteGetProperty(Properties properties, String propertyKey) {
        System.out.println("Property map: " + properties.getClass().getName() + "@" + Integer.toHexString(properties.hashCode()));
        System.out.println("Property requested: " + propertyKey);
        return "/dummy/java/home/path";
    }

    public static void rerouteStringSet(TestTransformable5 instance, String contents) {
        System.out.println("TestTransformable.foundProperty = " + contents);
    }
}
