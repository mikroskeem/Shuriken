package eu.mikroskeem.test.shuriken.instrumentation.testagent;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.instrumentation.Descriptor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Properties;

import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.FieldOpcode.PUT;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.MethodOpcode.VIRTUAL;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.findFieldInstruction;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.findMethodInvocation;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.rerouteFieldSetter;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.rerouteMethodInvocation;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findField;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findMethodWithAccessAtleast;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.readClass;

/**
 * @author Mark Vainomaa
 */
public class TestAgent5 implements ClassFileTransformer {
    private final static String TARGET_CL = "eu/mikroskeem/test/shuriken/instrumentation/testclasses/TestTransformable5";
    private final static int TARGET_A = Opcodes.ACC_PUBLIC;
    private final static String TARGET_M = "testProperty";
    private final static String TARGET_S = Descriptor.DEFAULT;

    private final static String ROUTE_CL = "java/util/Properties";
    private final static String ROUTE_M = "getProperty";
    private final static String ROUTE_S = new Descriptor().accepts(String.class).returns(String.class).build();

    private final static String ROUTE_T_CL = "eu/mikroskeem/test/shuriken/instrumentation/testclasses/TestRerouteTarget5";
    private final static String ROUTE_T_M_M = "rerouteGetProperty";
    private final static String ROUTE_T_M_S = new Descriptor().accepts(Properties.class, String.class).returns(String.class).build();
    private final static String ROUTE_T_F_M = "rerouteStringSet";
    private final static String ROUTE_T_F_S = new Descriptor().accepts(Type.getType("L" + TARGET_CL + ";"), Type.getType(String.class)).build();

    public static synchronized void agentmain(String args, Instrumentation instrumentation) throws Exception {
        new TestAgent5(instrumentation);
    }

    private TestAgent5(Instrumentation instrumentation) throws Exception {
        instrumentation.addTransformer(this, false);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(!className.equals(TARGET_CL))
            return classfileBuffer;
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassNode node = readClass(classfileBuffer);

            // Try to find target method
            MethodNode method = findMethodWithAccessAtleast(node, TARGET_A, TARGET_M, TARGET_S);
            Ensure.notNull(method, "Could not find target method!");

            // Find method invoke insn
            MethodInsnNode insnNode = findMethodInvocation(method.instructions, VIRTUAL, ROUTE_CL, ROUTE_M, ROUTE_S);
            Ensure.notNull(insnNode, "Could not find Properties.getProperty(String) call!");

            // Find field insn node
            FieldInsnNode fInsnNode = findFieldInstruction(method.instructions, PUT, TARGET_CL, "foundProperty", Type.getDescriptor(String.class));
            Ensure.notNull(fInsnNode, "Could not find TestTransformable.foundProperty setter");

            // Reroute method
            rerouteMethodInvocation(method.instructions, insnNode, ROUTE_T_CL, ROUTE_T_M_M, ROUTE_T_M_S);

            // Reroute field
            rerouteFieldSetter(method.instructions, fInsnNode, ROUTE_T_CL, ROUTE_T_F_M, ROUTE_T_F_S);

            node.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return classfileBuffer;
        }
    }
}
