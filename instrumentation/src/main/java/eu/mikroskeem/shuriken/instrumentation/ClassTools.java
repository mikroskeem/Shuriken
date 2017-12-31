package eu.mikroskeem.shuriken.instrumentation;

import eu.mikroskeem.shuriken.common.Ensure;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public final class ClassTools {
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
     * Generate simple <pre>super()</pre> calling constructor
     *
     * @param classVisitor ClassVisitor instance
     * @param superClass Super class name (use {@link Object} for non-extending classes
     *                   (or explictly extending Object, which is redundant anyway)
     */
    public static void generateSimpleSuperConstructor(@NotNull ClassVisitor classVisitor, @NotNull String superClass) {
        MethodVisitor mv = Ensure.notNull(classVisitor, "ClassWriter shouldn't be null!")
                .visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, unqualifyName(superClass), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
    }

    /**
     * Generate simple <pre>super()</pre> calling constructor
     *
     * @param classVisitor ClassWriter instance
     * @param superClass Super class object (use {@link Object} for non-extending classes
     *                   (or explictly extending Object, which is redundant anyway)
     */
    public static void generateSimpleSuperConstructor(@NotNull ClassVisitor classVisitor, @NotNull Class<?> superClass) {
        generateSimpleSuperConstructor(classVisitor, Ensure.notNull(superClass, "Class shouldn't be null").getName());
    }
}