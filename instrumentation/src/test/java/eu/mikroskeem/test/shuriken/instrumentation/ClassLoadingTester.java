package eu.mikroskeem.test.shuriken.instrumentation;

import eu.mikroskeem.shuriken.instrumentation.ClassLoaderTools;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Optional;

import static org.objectweb.asm.Opcodes.*;

public class ClassLoadingTester {
    @Test
    public void testClassLoading() throws Exception {
        SimpleClassLoader simpleClassLoader = new SimpleClassLoader(ClassLoader.getSystemClassLoader());

        String className = getClass().getPackage().getName()+".GeneratedTestClass";
        String classNameInternal = className.replaceAll("\\.", "/");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, classNameInternal, null, Type.getInternalName(Object.class), null);
        {
            FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, "test", Type.getDescriptor(String.class), null, "hey");
            fv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
        }
        cw.visitEnd();

        /* Test class loading */
        Class<?> generated = ClassLoaderTools.defineClass(simpleClassLoader, className, cw.toByteArray());
        Assertions.assertNotNull(generated, "Class didn't load!");
        Assertions.assertTrue(Reflect.getClass(className, simpleClassLoader).isPresent(),
                "Class isn't present in classloader!");
        Assertions.assertFalse(Reflect.getClass(className).isPresent()
                , "Class is present in system class loader!");

        /* Test field */
        ClassWrapper<?> c = Reflect.construct(Reflect.wrapClass(generated));
        Optional<FieldWrapper<String>> testFieldOptional = c.getField("test", String.class);
        Assertions.assertTrue(testFieldOptional.isPresent(), "Test field is not present!");
        Assertions.assertEquals("hey", testFieldOptional.get().read(), "Test field content didn't match!");
    }
}
