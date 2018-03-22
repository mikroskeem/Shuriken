package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.runtime.AgentFactory;
import eu.mikroskeem.test.shuriken.instrumentation.testagent.TestAgent;
import eu.mikroskeem.test.shuriken.instrumentation.testagent.TestAgent2;
import eu.mikroskeem.test.shuriken.instrumentation.testagent.TestAgent3;
import eu.mikroskeem.test.shuriken.instrumentation.testagent.TestAgent4;
import eu.mikroskeem.test.shuriken.instrumentation.testagent.TestAgent5;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestTransformable2;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestTransformable3;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestTransformable4;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestTransformable5;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
public class RuntimeTransformationTester {
    private static boolean agentFactoryWorks = false;

    @BeforeAll
    public static void testIfAgentFactoryWorks() throws Exception {
        try {
            AgentFactory.class.getName();
            agentFactoryWorks = true;
        } catch (Exception e) {
            System.out.println("Skipping runtime transformation unit tests, because they are not working with this JVM");
            e.printStackTrace();
        }
    }

    @Test
    public void testAgentLoading() throws Exception {
        Assumptions.assumeTrue(agentFactoryWorks, "Agent factory is not working");

        Path agentFile = AgentFactory.newJavaAgent(TestAgent.class);
        AgentFactory.attachAgent(agentFile);

        Assertions.assertEquals("present", System.getProperty("shuriken.testagent1"));
    }

    @Test
    public void testTransforming() throws Exception {
        Assumptions.assumeTrue(agentFactoryWorks, "Agent factory is not working");

        Path agentFile = AgentFactory.newJavaAgent(TestAgent2.class);
        AgentFactory.attachAgent(agentFile);

        // Check if method `a` is implemented
        TestTransformable2 test = new TestTransformable2();
        Method b = TestTransformable2.class.getMethod("a");
        Assertions.assertTrue(!Modifier.isAbstract(b.getModifiers()));

        // Check return value
        Assertions.assertEquals("foobarbaz", test.a());
    }

    @Test
    public void testRetransforming() throws Exception {
        Assumptions.assumeTrue(agentFactoryWorks, "Agent factory is not working");

        Path agentFile = AgentFactory.newJavaAgent(TestAgent3.class);

        // Define test array
        int[] testArray = new int[] { 0 };

        // Mutate first element (increment by one)
        TestTransformable3.incrementFirst(testArray);
        Assertions.assertEquals(1, testArray[0]);

        // Attach agent which retransforms that class
        AgentFactory.attachAgent(agentFile);

        // Mutate again and check if it is incremented by three
        TestTransformable3.incrementFirst(testArray);
        Assertions.assertEquals(4, testArray[0]);
    }

    @Test
    public void testRerouting() throws Exception {
        Assumptions.assumeTrue(agentFactoryWorks, "Agent factory is not working");

        AgentFactory.attachAgent(AgentFactory.newJavaAgent(TestAgent4.class));
        System.setProperty("shuriken.testagent4", "true");

        TestTransformable4 test = new TestTransformable4();
        test.testProperty();
        test.testPrint();
    }

    @Test
    public void testInstanceRerouting() throws Exception {
        Assumptions.assumeTrue(agentFactoryWorks, "Agent factory is not working");

        AgentFactory.attachAgent(AgentFactory.newJavaAgent(TestAgent5.class));

        TestTransformable5 test = new TestTransformable5();
        test.testProperty();
    }
}
