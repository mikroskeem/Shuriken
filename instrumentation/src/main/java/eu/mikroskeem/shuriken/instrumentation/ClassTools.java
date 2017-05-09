package eu.mikroskeem.shuriken.instrumentation;

import lombok.NonNull;
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
    public static String unqualifyName(@NonNull String className){
        return className.replace(".", "/");
    }

    /**
     * Unqualify class name
     *
     * @see #unqualifyName(String)
     * @param clazz Class
     * @return Unqualified class name
     */
    @NotNull
    public static String unqualifyName(@NonNull Class<?> clazz) {
        return unqualifyName(clazz.getName());
    }

    /**
     * Get class signature for generic class implementing/extending
     *
     * @param genericClass Generic class
     * @param types Generic types
     * @return Signature string
     */
    public static String getGenericSignature(Class<?> genericClass, Class<?>... types){
        String genericName = unqualifyName(genericClass.getName());
        StringBuilder sb = new StringBuilder();
        for(Class<?> type: types) sb.append("L").append(type.getSimpleName()).append(";");
        return "L" + genericName + "<" + sb.toString() + ">;";
    }

    /**
     * Generate simple <pre>super()</pre> calling constructor
     *
     * @param classWriter ClassWriter instance
     * @param superClass Super class (use {@link Object} for non-extending classes
     *                   (or explictly extending Object, which is redundant anyway)
     */
    public static void generateSimpleSuperConstructor(ClassWriter classWriter, Class<?> superClass){
        MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, unqualifyName(superClass), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}