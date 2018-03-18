package eu.mikroskeem.shuriken.instrumentation.runtime;

import eu.mikroskeem.shuriken.instrumentation.Descriptor;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.Instrumentation;
import java.util.List;

import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.findMethodWithAccessAtleast;
import static eu.mikroskeem.shuriken.instrumentation.bytecode.ClassManipulation.readClass;

/**
 * @author Mark Vainomaa
 */
final class AgentClassValidator {
    private final static Type STRING = Type.getType(String.class);
    private final static Type INSTRUMENTATION = Type.getType(Instrumentation.class);

    private final static String SIGNATURE = new Descriptor().accepts(STRING, INSTRUMENTATION).build();

    static void validateMainClass(@NotNull AgentJarOutputStream outputStream) {
        ClassNode classNode = readClass(outputStream.currentEntryData.toByteArray());
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;

        // Try to find agentmain(String, Instrumentation)
        MethodNode agentMain = findMethodWithAccessAtleast(methods, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "agentmain", SIGNATURE);
        if(agentMain == null) {
            throw new IllegalStateException("Agent class should have 'public static void agentmain(String, Instrumentation)' method!");
        }
    }
}
