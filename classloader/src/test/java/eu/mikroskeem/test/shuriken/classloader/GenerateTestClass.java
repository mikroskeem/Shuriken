package eu.mikroskeem.test.shuriken.classloader;

import eu.mikroskeem.shuriken.common.data.Pair;
import eu.mikroskeem.shuriken.instrumentation.ClassTools;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

/**
 * Generate test class
 *
 * @author Mark Vainomaa
 */
public class GenerateTestClass {
    public static Pair<String, byte[]> generate(){
        String className = "eu/mikroskeem/test/shuriken/classloader/classes/TestClass1";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, Type.getInternalName(Object.class), null);
        ClassTools.generateSimpleSuperConstructor(cw, Object.class);
        cw.visitEnd();
        return new Pair<>(className+".class.br", cw.toByteArray());
    }
}
