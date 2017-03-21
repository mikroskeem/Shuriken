package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.simple.SimpleReflect;
import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Stream;

import static eu.mikroskeem.shuriken.common.Ensure.notNull;
import static eu.mikroskeem.shuriken.reflect.wrappers.TypeWrapper.of;

/**
 * Class validation tools
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class Validate {
    /**
     * Private constructor, do not use
     */
    private Validate(){
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
        notNull(classBytes, "Class data shouldn't be null!");
        ClassReader cr = new ClassReader(classBytes);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            CheckClassAdapter.verify(cr, false, pw);
        }
        catch (Exception ignored){}
        if(sw.toString().length() > 0){
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
    public static void checkFields(@NonNull Class<?> clazz, FieldDescriptor... fields){
        ClassWrapper<?> cw = Reflect.wrapClass(clazz);
        Stream.of(fields).forEach(fieldDescriptor -> {
            try {
                Ensure.ensureCondition(
                        cw.getField(fieldDescriptor.getFieldName(), fieldDescriptor.getFieldType()).isPresent(),
                        NullPointerException.class,
                        of(String.format("Field %s %s not found",
                                fieldDescriptor.getFieldType(),
                                fieldDescriptor.getFieldName()))
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
    public static void checkMethods(@NonNull Class<?> clazz, MethodDescriptor... methods) {
        Stream.of(methods).forEach(methodDescriptor ->
                notNull(SimpleReflect.getMethod(clazz,
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
     * Check constructors availability in class against info defined in
     * {@link ConstructorDescriptor} objects
     *
     * @param clazz Class to perform check on
     * @param constructors {@link ConstructorDescriptor} objects
     */
    @Contract("null, _ -> fail")
    public static void checkConstructors(@NonNull Class<?> clazz, ConstructorDescriptor... constructors) {
        Stream.of(constructors).forEach(constructorDescriptor -> {
            try {
                notNull(clazz.getConstructor(constructorDescriptor.getArguments()),
                        String.format("Constructor (%s) not found",
                                Arrays.toString(constructorDescriptor.getArguments())
                        ));
            } catch (Exception e){
                throw new NullPointerException(e.getMessage());
            }
        });
    }

    /**
     * Checks class extending/implementation against info defined in
     * {@link ClassDescriptor} objects
     *
     * @param classDescriptor {@link ClassDescriptor} objects
     */
    @Contract("null -> fail")
    public static void checkClass(@NonNull ClassDescriptor classDescriptor){
        notNull(classDescriptor.getClazz(), "Class is null");
        Stream.of(classDescriptor.getExtendingClasses()).forEach(clazz->{
            if(!clazz.isAssignableFrom(classDescriptor.getClazz())){
                throw new NullPointerException(String.format("Class doesn't extend %s", clazz.getSimpleName()));
            }
        });
    }
}