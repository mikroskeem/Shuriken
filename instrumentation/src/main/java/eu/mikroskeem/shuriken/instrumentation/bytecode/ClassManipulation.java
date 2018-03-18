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
 * "Safer" class file manipulation
 *
 * @author Mark Vainomaa
 */
public final class ClassManipulation {
    /**
     * Reads class into {@link ClassNode} from raw data
     *
     * @param classData Class raw data
     * @param flags {@link ClassReader} flags
     * @return Instance of {@link ClassNode}, containing class data
     */
    @NotNull
    public static ClassNode readClass(@NotNull byte[] classData, int flags) {
        ClassReader classReader = new ClassReader(classData);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, flags);
        return classNode;
    }

    /**
     * Read class into {@link ClassNode} from raw data
     *
     * @param classData Class raw data
     * @return Instance of {@link ClassNode}, containing class data
     */
    @NotNull
    public static ClassNode readClass(@NotNull byte[] classData) {
        return readClass(classData, 0);
    }

    /**
     * Finds method from list of method nodes
     *
     * @param methodNodes Method nodes
     * @param predicate Predicate to test method node
     * @return Found method or null
     */
    @Nullable
    public static MethodNode findMethod(@NotNull List<MethodNode> methodNodes, @NotNull Predicate<MethodNode> predicate) {
        return methodNodes.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Finds method from {@link ClassNode}
     *
     * @param classNode Class node
     * @param predicate Predicate to test method
     * @return Found method or null
     */
    @Nullable
    public static MethodNode findMethod(@NotNull ClassNode classNode, @NotNull Predicate<MethodNode> predicate) {
        return findMethod((List<MethodNode>) classNode.methods, predicate);
    }

    /**
     * Finds method from list of method nodes
     *
     * @param methodNodes Method nodes
     * @param access Method exact access value
     * @param name Method exact name
     * @param desc Method descriptor
     * @return Found method or null
     */
    @Nullable
    public static MethodNode findMethod(@NotNull List<MethodNode> methodNodes, int access, @NotNull String name, @NotNull String desc) {
        return findMethod(methodNodes, m -> access == m.access && name.equals(m.name) && desc.equals(m.desc));
    }

    /**
     * Finds method from {@link ClassNode}
     *
     * @param classNode Class node
     * @param access Method exact access value
     * @param name Method exact name
     * @param desc Method descriptor
     * @return Found method or null
     */
    @Nullable
    public static MethodNode findMethod(@NotNull ClassNode classNode, int access, @NotNull String name, @NotNull String desc) {
        return findMethod((List<MethodNode>) classNode.methods, access, name, desc);
    }

    /**
     * Finds method from list of method nodes with access value atleast of {@code access}
     *
     * @param methodNodes Method nodes
     * @param access Access value what method should have
     * @param name Method exact name
     * @param desc Method descriptor
     * @return Found method or null
     */
    @Nullable
    public static MethodNode findMethodWithAccessAtleast(@NotNull List<MethodNode> methodNodes, int access, @NotNull String name, @NotNull String desc) {
        return findMethod(methodNodes, m -> (m.access & access) != 0 && name.equals(m.name) && desc.equals(m.desc));
    }

    /**
     * Finds method from {@link ClassNode} with access value atleast of {@code access}
     *
     * @param classNode Class node
     * @param access Access value what method should have
     * @param name Method exact name
     * @param desc Method descriptor
     * @return Found method or null
     */
    @Nullable
    public static MethodNode findMethodWithAccessAtleast(@NotNull ClassNode classNode, int access, @NotNull String name, @NotNull String desc) {
        return findMethodWithAccessAtleast((List<MethodNode>) classNode.methods, access, name, desc);
    }

    /**
     * Finds field from list of field nodes
     *
     * @param fieldNodes Field nodes
     * @param predicate Predicate to test field node
     * @return Found field or null
     */
    @Nullable
    public static FieldNode findField(@NotNull List<FieldNode> fieldNodes, @NotNull Predicate<FieldNode> predicate) {
        return fieldNodes.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Finds field from {@link ClassNode}
     *
     * @param classNode Class node
     * @param predicate Predicate to test field node
     * @return Found field or null
     */
    @Nullable
    public static FieldNode findField(@NotNull ClassNode classNode, @NotNull Predicate<FieldNode> predicate) {
        return findField((List<FieldNode>) classNode.fields, predicate);
    }

    /**
     * Finds field from list of field nodes
     *
     * @param fieldNodes Field nodes
     * @param access Field exact access value
     * @param name Field exact name
     * @param desc Field type descriptor
     * @return Found field or null
     */
    @Nullable
    public static FieldNode findField(@NotNull List<FieldNode> fieldNodes, int access, @NotNull String name, @NotNull String desc) {
        return findField(fieldNodes, f -> access == f.access && name.equals(f.name) && desc.equals(f.desc));
    }

    /**
     * Finds field from {@link ClassNode}
     *
     * @param classNode Class node
     * @param access Field exact access value
     * @param name Field exact name
     * @param desc Field type descriptor
     * @return Found field or null
     */
    @Nullable
    public static FieldNode findField(@NotNull ClassNode classNode, int access, @NotNull String name, @NotNull String desc) {
        return findField((List<FieldNode>) classNode.fields, f -> access == f.access && name.equals(f.name) && desc.equals(f.desc));
    }

    /**
     * Finds field from list of field nodes with access value atleast of {@code access}
     *
     * @param fieldNodes Field nodes
     * @param access Access value what field should have
     * @param name Field exact name
     * @param desc Field type descriptor
     * @return Found field or null
     */
    @Nullable
    public static FieldNode findFieldWithAccessAtleast(@NotNull List<FieldNode> fieldNodes, int access, @NotNull String name, @NotNull String desc) {
        return findField(fieldNodes, f -> (f.access & access) != 0 && name.equals(f.name) && desc.equals(f.desc));
    }

    /**
     * Finds field from {@link ClassNode} with access value atleast of {@code access}
     *
     * @param classNode Class node
     * @param access Access value what field should have
     * @param name Field exact name
     * @param desc Field type descriptor
     * @return Found field or null
     */
    @Nullable
    public static FieldNode findFieldWithAccessAtleast(@NotNull ClassNode classNode, int access, @NotNull String name, @NotNull String desc) {
        return findFieldWithAccessAtleast((List<FieldNode>) classNode.fields, access, name, desc);
    }
}
