package eu.mikroskeem.test.shuriken.instrumentation;


import eu.mikroskeem.shuriken.instrumentation.validate.ClassDescriptor;
import eu.mikroskeem.shuriken.instrumentation.validate.ConstructorDescriptor;
import eu.mikroskeem.shuriken.instrumentation.validate.MethodDescriptor;
import eu.mikroskeem.shuriken.instrumentation.validate.Validate;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass;
import eu.mikroskeem.test.shuriken.instrumentation.testclasses.TestClass2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class ValidatorTester {
    @Test
    public void testClassValidator() throws Exception {
        ClassDescriptor cd = ClassDescriptor.of(TestClass.class, Object.class);
        ClassDescriptor cd2 = ClassDescriptor.of(TestClass2.class, TestClass.class);
        Validate.checkClass(cd);
        Validate.checkClass(cd2);
    }

    @Test
    public void testBytecodeValidator() throws Exception {
        byte[] clazzContents = BytecodeDump.INST.generate();
        Assertions.assertEquals(clazzContents, Validate.checkGeneratedClass(clazzContents));

        byte[] invalidClazzContents = InvalidBytecodeDump.INST.generate();
        ClassFormatError e = Assertions.assertThrows(ClassFormatError.class, () ->
            Assertions.assertNotEquals(invalidClazzContents, Validate.checkGeneratedClass(invalidClazzContents))
        );
    }

    @Test
    public void testMethodValidator() throws Exception {
        Validate.checkMethods(TestClass.class, MethodDescriptor.of("a", void.class));
        Validate.checkMethods(TestClass.class, MethodDescriptor.of("b", String.class));
        Validate.checkMethods(TestClass.class, MethodDescriptor.of("c", boolean.class, int.class, byte.class, char.class));
    }

    @Test
    public void testInvalidMethodValidator() throws Exception {
        Assertions.assertThrows(NullPointerException.class, ()->
            Validate.checkMethods(TestClass.class, MethodDescriptor.of("e", int.class, int.class))
        );
    }

    @Test
    public void testConstructorValidator() throws Exception {
        Validate.checkConstructors(TestClass2.class,
                ConstructorDescriptor.of(), // No args constructor
                ConstructorDescriptor.of(String.class, String.class),
                ConstructorDescriptor.of(String.class, int.class)
        );
    }

    @Test
    public void testConstructorValidatorWithReflectiveClasses() throws Exception {
        ClassWrapper<?> clazz = Reflect.getClass("java.lang.String").get();
        Validate.checkConstructors(TestClass2.class,
                ConstructorDescriptor.ofWrapped(clazz, clazz)
        );
    }

    @Test
    public void testInvalidConstructorValidator() throws Exception {
        NullPointerException e = Assertions.assertThrows(NullPointerException.class, ()->{
            Validate.checkConstructors(TestClass2.class,
                    ConstructorDescriptor.of(char.class)
            );
        });
    }

    /* Class generators */
    static class BytecodeDump {
        static BytecodeDump INST = new BytecodeDump();
        public byte[] generate(){
            String className = "eu.mikroskeem.test.shuriken.common.classtools.gen.GeneratedClass";
            String classNameInternal = className.replaceAll("\\.", "/");

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
            cw.visit(
                    V1_8,
                    ACC_PUBLIC + ACC_SUPER,
                    classNameInternal,
                    null,
                    Type.getInternalName(Object.class),
                    null);
            FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, "test", Type.getDescriptor(String.class), null, "hey");
            fv.visitEnd();
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
            return cw.toByteArray();
        }
    }

    static class InvalidBytecodeDump {
        static InvalidBytecodeDump INST = new InvalidBytecodeDump();
        public byte[] generate(){
            String className = "eu.mikroskeem.test.shuriken.common.classtools.gen.InvalidGeneratedClass";
            String classNameInternal = className.replaceAll("\\.", "/");

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
            cw.visit(
                    V1_8,
                    ACC_PUBLIC + ACC_SUPER,
                    classNameInternal,
                    null,
                    Type.getInternalName(Object.class),
                    null);
            FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, "test", Type.getDescriptor(String.class), null, "hey");
            fv.visitEnd();
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
            return cw.toByteArray();
        }
    }
}
