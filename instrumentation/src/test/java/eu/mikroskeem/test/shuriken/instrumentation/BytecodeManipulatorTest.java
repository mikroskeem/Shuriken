package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.ClassLoaderTools;
import eu.mikroskeem.shuriken.instrumentation.ClassTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static eu.mikroskeem.shuriken.common.streams.ByteArrays.fromInputStream;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findField;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findMethod;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.readClass;

/**
 * @author Mark Vainomaa
 */
public class BytecodeManipulatorTest {
    private static byte[] testClassData;
    private static Type tc11;

    @BeforeAll
    private static void before() throws Exception {
        Class<?> clazz = Class.forName("eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass10");
        Class<?> clazz2 = Class.forName("eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass11");
        testClassData = fromInputStream(clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class"));
        tc11 = Type.getType(clazz2);
    }

    @Test
    public void testFieldFinding() throws Exception {
        ClassNode classNode = readClass(testClassData);

        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;

        FieldNode foundField = findField(fields, Opcodes.ACC_PRIVATE, "a", "Ljava/lang/String;");
        FieldNode foundFieldTwo = findField(fields, Opcodes.ACC_PRIVATE, "tc11", tc11.getDescriptor());

        Assertions.assertNotNull(foundField);
        Assertions.assertNotNull(foundFieldTwo);
        Assertions.assertEquals(fields.get(0), foundField);
        Assertions.assertEquals(fields.get(1), foundFieldTwo);
    }

    @Test
    public void testMethodFinding() throws Exception {
        ClassNode classNode = readClass(testClassData);

        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;

        MethodNode foundMethod = findMethod(methods, 0, "<init>", "()V");
        MethodNode foundMethodTwo = findMethod(methods, Opcodes.ACC_PRIVATE, "b", "()" + tc11.getDescriptor());

        Assertions.assertNotNull(foundMethod);
        Assertions.assertNotNull(foundMethodTwo);
        Assertions.assertEquals(methods.get(0), foundMethod);
        Assertions.assertEquals(methods.get(1), foundMethodTwo);
    }
}
