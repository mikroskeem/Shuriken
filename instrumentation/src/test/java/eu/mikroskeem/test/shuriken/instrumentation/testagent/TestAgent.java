package eu.mikroskeem.test.shuriken.instrumentation.testagent;

import java.lang.instrument.Instrumentation;

/**
 * @author Mark Vainomaa
 */
public class TestAgent {
    public static synchronized void agentmain(String args, Instrumentation instrumentation) throws Exception {
        System.setProperty("shuriken.testagent1", "present");
    }
}
