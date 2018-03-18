package eu.mikroskeem.shuriken.instrumentation.bytecode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author Mark Vainomaa
 */
public final class ClassManipulation {
    @NotNull
    public static ClassNode readClass(@NotNull byte[] classData, int flags) {
        ClassReader classReader = new ClassReader(classData);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, flags);
        return classNode;
    }

    @NotNull
    public static ClassNode readClass(@NotNull byte[] classData) {
        return readClass(classData, 0);
    }

    @Nullable
    public static MethodNode findMethod(@NotNull List<MethodNode> methodNodes, @NotNull Predicate<MethodNode> predicate) {
        return methodNodes.stream().filter(predicate).findFirst().orElse(null);
    }

    @Nullable
    public static MethodNode findMethod(@NotNull List<MethodNode> methodNodes, int access, @NotNull String name, @NotNull String desc) {
        return findMethod(methodNodes, m -> access == m.access && name.equals(m.name) && desc.equals(m.desc));
    }

    @Nullable
    public static MethodNode findMethodWithAccessAtleast(@NotNull List<MethodNode> methodNodes, int access, @NotNull String name, @NotNull String desc) {
        return findMethod(methodNodes, m -> (m.access & access) != 0 && name.equals(m.name) && desc.equals(m.desc));
    }

    @Nullable
    public static FieldNode findField(@NotNull List<FieldNode> fieldNodes, @NotNull Predicate<FieldNode> predicate) {
        return fieldNodes.stream().filter(predicate).findFirst().orElse(null);
    }

    @Nullable
    public static FieldNode findField(@NotNull List<FieldNode> fieldNodes, int access, @NotNull String name, @NotNull String desc) {
        return findField(fieldNodes, f -> access == f.access && name.equals(f.name) && desc.equals(f.desc));
    }

    @Nullable
    public static FieldNode findFieldWithAccessAtleast(@NotNull List<FieldNode> fieldNodes, int access, @NotNull String name, @NotNull String desc) {
        return findField(fieldNodes, f -> (f.access & access) != 0 && name.equals(f.name) && desc.equals(f.desc));
    }
}
