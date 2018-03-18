package eu.mikroskeem.test.shuriken.instrumentation.testagent;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.instrumentation.Descriptor;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Properties;

import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.FieldOpcode.GETSTATIC;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.MethodOpcode.STATIC;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.findFieldInstruction;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.findMethodInvocation;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.rerouteFieldGetter;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.rerouteMethodInvocation;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findMethodWithAccessAtleast;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.readClass;

/**
 * @author Mark Vainomaa
 */
public class TestAgent4 implements ClassFileTransformer {
    private final static String TARGET_CL = "eu/mikroskeem/test/shuriken/instrumentation/testclasses/TestTransformable4";
    private final static int TARGET_A = Opcodes.ACC_PUBLIC;
    private final static String TARGET_M = "testProperty";
    private final static String TARGET_M2 = "testPrint";
    private final static String TARGET_S = Descriptor.DEFAULT;

    private final static String ROUTE_CL = "java/lang/System";
    private final static String ROUTE_M = "getProperties";
    private final static String ROUTE_S = new Descriptor().returns(Properties.class).build();

    private final static String ROUTE_F = "out";
    private final static String ROUTE_F_T = Type.getDescriptor(PrintStream.class);
    private final static String ROUTE_F_T_M = new Descriptor().returns(PrintStream.class).build();

    public static synchronized void agentmain(String args, Instrumentation instrumentation) throws Exception {
        new TestAgent4(instrumentation);
    }

    private TestAgent4(Instrumentation instrumentation) throws Exception {
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

            MethodNode method2 = findMethodWithAccessAtleast(node, TARGET_A, TARGET_M2, TARGET_S);
            Ensure.notNull(method2, "Could not find target method 2!");

            // Find method invoke insn
            MethodInsnNode insnNode = findMethodInvocation(method.instructions, STATIC, ROUTE_CL, ROUTE_M, ROUTE_S);
            Ensure.notNull(insnNode, "Could not find System.getProperties() call!");

            // Find field get insn
            FieldInsnNode fInsnNode = findFieldInstruction(method2.instructions, GETSTATIC, ROUTE_CL, ROUTE_F, ROUTE_F_T);
            Ensure.notNull(fInsnNode, "Could not find GETSTATIC System.out");

            // Reroute method
            rerouteMethodInvocation(method.instructions, insnNode, TARGET_CL, "getDummyProperties", ROUTE_S);

            // Reroute field
            rerouteFieldGetter(method2.instructions, fInsnNode, TARGET_CL, "getDummyStream", ROUTE_F_T_M);

            node.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return classfileBuffer;
        }
    }
}
