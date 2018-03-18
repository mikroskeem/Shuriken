package eu.mikroskeem.test.shuriken.instrumentation.testagent;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.instrumentation.Descriptor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static eu.mikroskeem.shuriken.instrumentation.bytecode.BytecodeManipulation.findInstruction;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findMethod;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.readClass;

/**
 * @author Mark Vainomaa
 */
public class TestAgent3 implements ClassFileTransformer {
    private final static String TARGET_CL = "eu/mikroskeem/test/shuriken/instrumentation/testclasses/TestTransformable3";
    private final static int TARGET_A = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC;
    private final static String TARGET_M = "incrementFirst";
    private final static String TARGET_S = new Descriptor().accepts(int[].class).build();

    public static synchronized void agentmain(String args, Instrumentation instrumentation) throws Exception {
        new TestAgent3(instrumentation);
    }

    private TestAgent3(Instrumentation instrumentation) throws Exception {
        instrumentation.addTransformer(this, true);
        Class<?> target = Class.forName(TARGET_CL.replace('/', '.'));
        instrumentation.retransformClasses(target);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(!className.equals(TARGET_CL))
            return classfileBuffer;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassNode node = readClass(classfileBuffer);

        // Try to find target method
        MethodNode method = findMethod(node, TARGET_A, TARGET_M, TARGET_S);
        Ensure.notNull(method, "Could not find target method!");

        // Find ICONST_1 instruction (note: very shitty way, but then again since it's only usage so it's fine)
        // Note: x++ --> x = x + 1
        InsnNode insnNode = findInstruction(method.instructions, InsnNode.class, insn -> insn.getOpcode() == Opcodes.ICONST_1);
        Ensure.notNull(insnNode, "Could not find target instruction!");

        // Replace instruction with new one (x + 1 --> x + 3)
        method.instructions.set(insnNode, new InsnNode(Opcodes.ICONST_3));

        node.accept(cw);
        return cw.toByteArray();
    }
}
