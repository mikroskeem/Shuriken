package eu.mikroskeem.shuriken.instrumentation;

import eu.mikroskeem.shuriken.common.Ensure;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class ClassTools {
    /**
     * Unqualify class name <br>
     * In other words, <pre>foo.bar.baz</pre> -&gt; <pre>foo/bar/baz</pre>
     *
     * @param className Class name
     * @return Unqualified class name
     */
    @NotNull
    public static String unqualifyName(String className) {
        return Ensure.notNull(className, "Class name shouldn't be null!").replace(".", "/");
    }

    /**
     * Unqualify class name
     *
     * @see #unqualifyName(String)
     * @param clazz Class
     * @return Unqualified class name
     */
    @NotNull
    public static String unqualifyName(Class<?> clazz) {
        return unqualifyName(Ensure.notNull(clazz, "Class shouldn't be null!").getName());
    }

    /**
     * Get class signature for generic class implementing/extending
     *
     * @param genericClass Generic class
     * @param types Generic types
     * @return Signature string
     */
    @NotNull
    public static String getGenericSignature(Class<?> genericClass, Class<?>... types) {
        String genericName = unqualifyName(Ensure.notNull(genericClass, "Class shouldn't be null!").getName());
        StringBuilder sb = new StringBuilder();
        for(Class<?> type: types) sb.append("L").append(type.getSimpleName()).append(";");
        return "L" + genericName + "<" + sb.toString() + ">;";
    }

    /**
     * Generate simple <pre>super()</pre> calling constructor
     *
     * @param classWriter ClassWriter instance
     * @param superClass Super class name (use {@link Object} for non-extending classes
     *                   (or explictly extending Object, which is redundant anyway)
     */
    public static void generateSimpleSuperConstructor(@NotNull ClassWriter classWriter, @NotNull String superClass) {
        MethodVisitor mv = Ensure.notNull(classWriter, "ClassWriter shouldn't be null!")
                .visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, unqualifyName(superClass), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
    }

    /**
     * Generate simple <pre>super()</pre> calling constructor
     *
     * @param classWriter ClassWriter instance
     * @param superClass Super class object (use {@link Object} for non-extending classes
     *                   (or explictly extending Object, which is redundant anyway)
     */
    public static void generateSimpleSuperConstructor(@NotNull ClassWriter classWriter, @NotNull Class<?> superClass) {
        generateSimpleSuperConstructor(classWriter, Ensure.notNull(superClass, "Class shouldn't be null").getName());
    }
}