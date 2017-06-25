package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Class validation tools
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public final class Validate {
    /**
     * Private constructor, do not use
     */
    private Validate() {
        throw new RuntimeException("No Validate instance for you!");
    }

    /**
     * Verify class bytecode
     *
     * @param classBytes Class data
     * @return Class data, if it was valid
     * @throws ClassFormatError If class wasn't valid
     */
    @Contract("null -> fail")
    public static byte[] checkGeneratedClass(byte[] classBytes) throws ClassFormatError {
        Ensure.notNull(classBytes, "Class data shouldn't be null!");
        ClassReader cr = new ClassReader(classBytes);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            CheckClassAdapter.verify(cr, false, pw);
        }
        catch (Exception ignored) {}
        if(sw.toString().length() > 0) {
            throw new ClassFormatError(sw.toString());
        }
        return classBytes;
    }

    /**
     * Check fields availability in class agains info defined in
     * {@link FieldDescriptor} objects
     *
     * @param clazz Class to perform check on
     * @param fields {@link FieldDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkFields(Class<?> clazz, FieldDescriptor... fields) {
        ClassWrapper<?> cw = Reflect.wrapClass(clazz);
        Stream.of(fields).forEach(fieldDescriptor -> {
            try {
                Ensure.ensurePresent(
                        cw.getField(fieldDescriptor.getFieldName(), fieldDescriptor.getFieldType()),
                        String.format("Field %s %s not found",
                                fieldDescriptor.getFieldType(),
                                fieldDescriptor.getFieldName())
                );
            } catch (Exception e) {
                throw new NullPointerException(e.getLocalizedMessage());
            }
        });
    }

    /**
     * Check fields availability in wrapped class agains info defined in
     * {@link FieldDescriptor} objects
     *
     * @param cw Wrapped class to perform check on
     * @param fields {@link FieldDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkFields(ClassWrapper<?> cw, FieldDescriptor... fields) {
        Ensure.notNull(cw, "ClassWrapper shouldn't be null!");
        Stream.of(fields).forEach(fieldDescriptor -> {
            try {
                Ensure.ensurePresent(
                        cw.getField(fieldDescriptor.getFieldName(), fieldDescriptor.getFieldType()),
                        String.format("Field %s %s not found",
                                fieldDescriptor.getFieldType(),
                                fieldDescriptor.getFieldName())
                );
            } catch (Exception e) {
                throw new NullPointerException(e.getLocalizedMessage());
            }
        });
    }


    /**
     * Check methods availability in class against info defined in
     * {@link MethodDescriptor} objects
     *
     * @param clazz Class to perform check on
     * @param methods {@link MethodDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkMethods(Class<?> clazz, MethodDescriptor... methods) {
        Stream.of(methods).forEach(methodDescriptor ->
                Ensure.notNull(getMethod(clazz,
                        methodDescriptor.getMethodName(),
                        methodDescriptor.getReturnType(),
                        methodDescriptor.getArguments()),
                        String.format("Method %s(%s) not found",
                                methodDescriptor.getMethodName(),
                                Arrays.toString(methodDescriptor.getArguments())
                        ))
        );
    }

    /**
     * Check methods availability in wrapped class against info defined in
     * {@link MethodDescriptor} objects
     *
     * @param cw Wrapped class to perform check on
     * @param methods {@link MethodDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkMethods(ClassWrapper<?> cw, MethodDescriptor... methods) {
        checkMethods(Ensure.notNull(cw, "ClassWrapper shouldn't be null!").getWrappedClass(), methods);
    }

    /**
     * Check constructors availability in class against info defined in
     * {@link ConstructorDescriptor} objects
     *
     * @param clazz Class to perform check on
     * @param constructors {@link ConstructorDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkConstructors(Class<?> clazz, ConstructorDescriptor... constructors) {
        Ensure.notNull(clazz, "ClassWrapper shouldn't be null!");
        Stream.of(constructors).forEach(constructorDescriptor -> {
            try {
                Ensure.notNull(clazz.getConstructor(constructorDescriptor.getArguments()),
                        String.format("Constructor (%s) not found",
                                Arrays.toString(constructorDescriptor.getArguments())
                        ));
            } catch (Exception e) {
                throw new NullPointerException(e.getMessage());
            }
        });
    }

    /**
     * Check constructors availability in wrapped class against info defined in
     * {@link ConstructorDescriptor} objects
     *
     * @param cw Wrapped class to perform check on
     * @param constructors {@link ConstructorDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkConstructors(ClassWrapper<?> cw, ConstructorDescriptor... constructors) {
        checkConstructors(Ensure.notNull(cw, "ClassWrapper shouldn't be null!").getWrappedClass(), constructors);
    }

    /**
     * Checks class extending/implementation against info defined in
     * {@link ClassDescriptor} objects
     *
     * @param classDescriptor {@link ClassDescriptor} objects
     */
    @Contract("null -> fail")
    public static void checkClass(ClassDescriptor classDescriptor) {
        Ensure.notNull(classDescriptor.getDescribedClass(), "Class is null");
        Stream.of(classDescriptor.getExtendingClasses()).forEach(clazz->{
            if(!clazz.isAssignableFrom(classDescriptor.getDescribedClass())) {
                throw new NullPointerException(String.format("Class doesn't extend %s", clazz.getSimpleName()));
            }
        });
    }

    @Nullable
    @Contract("null, null, null, _ -> fail")
    private static Method getMethod(Class<?> clazz, String method, Class<?> returnType, Class<?>... arguments) {
        Ensure.notNull(clazz, "Class shouldn't be null!");
        Ensure.notNull(method, "Method name shouldn't be null!");
        Ensure.notNull(returnType, "Return type shouldn't be null!");
        try {
            Method m = clazz.getDeclaredMethod(method, arguments);
            m.setAccessible(true);
            if(m.getReturnType() != returnType) throw new NoSuchMethodException();
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}