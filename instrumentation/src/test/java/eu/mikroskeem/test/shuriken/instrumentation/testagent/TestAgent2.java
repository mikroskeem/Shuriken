package eu.mikroskeem.test.shuriken.instrumentation.testagent;

import eu.mikroskeem.shuriken.instrumentation.Descriptor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * @author Mark Vainomaa
 */
public class TestAgent2 implements ClassFileTransformer {
    private final static String TARGET_CL = "eu/mikroskeem/test/shuriken/instrumentation/testclasses/TestTransformable2";
    private final static int TARGET_A = Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER;
    private final static String TARGET_M = "a";
    private final static String TARGET_S = Descriptor.newDescriptor().returns(String.class).build();

    public static synchronized void agentmain(String args, Instrumentation instrumentation) throws Exception {
        new TestAgent2(instrumentation);
    }

    private TestAgent2(Instrumentation instrumentation) throws Exception {
        instrumentation.addTransformer(this, false);
        //Class<?> target = Class.forName(TARGET_CL.replace('/', '.'));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(!className.equals(TARGET_CL))
            return classfileBuffer;

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cr.accept(cw, 0);

        // Implement method `a`
        MethodVisitor mv = cw.visitMethod(TARGET_A, TARGET_M, TARGET_S, null, null);
        GeneratorAdapter ga = new GeneratorAdapter(mv, TARGET_A, TARGET_M, TARGET_S);
        ga.visitCode();
        ga.visitLdcInsn("foobarbaz");
        ga.returnValue();
        ga.visitMaxs(0, 0);
        ga.visitEnd();

        return cw.toByteArray();
    }
}
