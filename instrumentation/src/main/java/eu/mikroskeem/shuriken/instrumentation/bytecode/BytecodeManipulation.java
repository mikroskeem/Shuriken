package eu.mikroskeem.shuriken.instrumentation.bytecode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static eu.mikroskeem.shuriken.common.collections.CollectionUtilities.firstOrNull;

/**
 * "Safer" bytecode manipulation utilities
 *
 * @author Mark Vainomaa
 */
public final class BytecodeManipulation {
    /**
     * Tries to find an instructions from instruction list
     *
     * @param instructions Instructions list
     * @param instructionType Instruction type to find
     * @param predicate Instruction test predicate
     * @param <T> Instruction type
     * @return Found instructions or empty list, if didn't find anything
     */
    @NotNull
    public static <T extends AbstractInsnNode> List<T> findInstructions(@NotNull InsnList instructions,
                                                                        @NotNull Class<T> instructionType,
                                                                        @NotNull Predicate<T> predicate) {
        return Arrays.stream(instructions.toArray())
                .filter(i -> i.getClass() == instructionType)
                .map(instructionType::cast)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * Tries to find an instruction from instruction list
     *
     * @param instructions Instruction list
     * @param instructionType Instruction type
     * @param predicate Instruction test predicate
     * @param <T> Instruction type
     * @return First target instruction, or null if not found
     */
    @Nullable
    public static <T extends AbstractInsnNode> T findInstruction(@NotNull InsnList instructions,
                                                                 @NotNull Class<T> instructionType,
                                                                 @NotNull Predicate<T> predicate) {
        return firstOrNull(findInstructions(instructions, instructionType, predicate));
    }

    /**
     * Tries to find type instantiations (in other words, {@code new Foo();})
     *
     * @param insnstructions Instructions list
     * @param type Type which is instantiated
     * @return All instructions instantiating {@code type} or empty list, if didn't find anything
     */
    @NotNull
    public static List<TypeInsnNode> findTypeInstantiations(@NotNull InsnList insnstructions, @NotNull String type) {
        return findInstructions(insnstructions, TypeInsnNode.class, i -> i.getOpcode() == Opcodes.NEW && type.equals(i.desc));
    }

    /**
     * Tries to find type instantiation (in other words, {@code new Foo();})
     *
     * @param insnstructions Instructions list
     * @param type Type which is instantiated
     * @return First instruction instantiating {@code type}
     */
    @Nullable
    public static TypeInsnNode findTypeInstantiation(@NotNull InsnList insnstructions, @NotNull String type) {
        return firstOrNull(findTypeInstantiations(insnstructions, type));
    }

    /**
     * Reroutes type instantiation instruction to static method
     *
     * Note: no access or parameter type casting checks are done!
     *
     * @param instructions Instructions list
     * @param typeInsnNode Type instatiation node
     * @param owner Reroute method owner class (in internal class name format, like {@code foo/bar/Baz})
     * @param name Reroute method name
     * @param desc Reroute method descriptor
     */
    public static void rerouteTypeInstantiation(@NotNull InsnList instructions,
                                                @NotNull TypeInsnNode typeInsnNode,
                                                @NotNull String owner,
                                                @NotNull String name,
                                                @NotNull String desc) {
        if(!instructions.contains(typeInsnNode))
            throw new IllegalStateException("Given instructions list does not contain provided instruction!");

        Type[] originalDesc = Type.getArgumentTypes(typeInsnNode.desc);
        Type[] targetDesc = Type.getArgumentTypes(desc);

        if(originalDesc.length != targetDesc.length)
            throw new IllegalStateException("Target method's parameter count must match with constructor!");

        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
        instructions.set(typeInsnNode, newInsn);
    }

    /**
     * Tries to find method invocations (in other words, {@code x.foo();})
     *
     * @param insnstructions Instructions list
     * @param methodOpcode Method opcode, see {@link MethodOpcode}
     * @param owner Method owner class
     * @param name Method name
     * @param desc Method descriptor
     * @return Method invocation instructions or empty list, if didn't find anything
     */
    @NotNull
    public static List<MethodInsnNode> findMethodInvocations(@NotNull InsnList insnstructions,
                                                      @NotNull MethodOpcode methodOpcode,
                                                      @NotNull String owner,
                                                      @NotNull String name,
                                                      @NotNull String desc) {
        return findInstructions(insnstructions, MethodInsnNode.class, m -> m.getOpcode() == methodOpcode.opcode &&
                owner.equals(m.owner) &&
                name.equals(m.name) &&
                desc.equals(m.desc) &&
                (methodOpcode.opcode != Opcodes.INVOKEINTERFACE || m.itf)
        );
    }

    /**
     * Tries to find method invocation (in other words, {@code x.foo();})
     *
     * @param insnstructions Instructions list
     * @param methodOpcode Method opcode, see {@link MethodOpcode}
     * @param owner Method owner class
     * @param name Method name
     * @param desc Method descriptor
     * @return First method invocation instruction or null
     */
    @Nullable
    public static MethodInsnNode findMethodInvocation(@NotNull InsnList insnstructions,
                                                      @NotNull MethodOpcode methodOpcode,
                                                      @NotNull String owner,
                                                      @NotNull String name,
                                                      @NotNull String desc) {
        return firstOrNull(findMethodInvocations(insnstructions, methodOpcode, owner, name, desc));
    }

    /**
     * Reroutes method invocation to static method
     *
     * Note: Rerouteable method must be static as well for now!
     *
     * @param instructions Instructions list
     * @param methodInsn Method instruction
     * @param owner Reroute method owner class (in internal class name format, like {@code foo/bar/Baz})
     * @param name Reroute method name
     * @param desc Reroute method descriptor
     */
    public static void rerouteMethodInvocation(@NotNull InsnList instructions,
                                               @NotNull MethodInsnNode methodInsn,
                                               @NotNull String owner,
                                               @NotNull String name,
                                               @NotNull String desc) {
        // Do some validations, since people like do dumb stuff and I don't want bytecode manipulation to be unsafe
        if(!instructions.contains(methodInsn))
            throw new IllegalStateException("Given instructions list does not contain provided instruction!");

        // TODO: :(
        if(methodInsn.getOpcode() != Opcodes.INVOKESTATIC)
            throw new IllegalStateException("Only static method invocation rerouting is supported for now!");

        Type[] originalDesc = Type.getArgumentTypes(methodInsn.desc);
        Type[] targetDesc = Type.getArgumentTypes(desc);

        if(originalDesc.length != targetDesc.length)
            throw new IllegalStateException("Target method's parameter count must match with original!");

        // TODO: Check original & target descriptor compatibility
        // TODO: Check if target method is accessible
        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
        instructions.set(methodInsn, newInsn);
    }

    /**
     * Reroutes method invocation to static method
     *
     * Note: Rerouteable method must be static as well for now!
     *
     * @param instructions Instructions list
     * @param methodInsn Method instruction
     * @param target Target method
     */
    public static void rerouteMethodInvocation(@NotNull InsnList instructions,
                                               @NotNull MethodInsnNode methodInsn,
                                               @NotNull Method target) {
        int modifiers = target.getModifiers();
        if(!Modifier.isStatic(modifiers))
            throw new IllegalStateException("Only static methods are supported for rerouting!");
        String owner = target.getDeclaringClass().getName().replace('.', '/');
        String desc = Type.getMethodDescriptor(target);
        rerouteMethodInvocation(instructions, methodInsn, owner, target.getName(), desc);
    }

    /**
     * Tries to find field instructions (in other words, {@code X a = getXSomewhere(); a.getFoo(); })
     *
     * @param insnstructions Instructions list
     * @param fieldOpcode Field opcode, see {@link FieldOpcode}
     * @param owner Field owner class (in internal class name format, like {@code foo/bar/Baz})
     * @param fieldName Field name
     * @param fieldType Field type in descriptor format, like {@code Ljava/lang/String;}
     * @return Found instructions list or empty list, if didn't find anything
     */
    @NotNull
    public static List<FieldInsnNode> findFieldInstructions(@NotNull InsnList insnstructions,
                                                     @NotNull FieldOpcode fieldOpcode,
                                                     @NotNull String owner,
                                                     @NotNull String fieldName,
                                                     @NotNull String fieldType) {
        return findInstructions(insnstructions, FieldInsnNode.class, f -> fieldOpcode.opcode == f.getOpcode() &&
                owner.equals(f.owner) && fieldName.equals(f.name) && fieldType.equals(f.desc)
        );
    }

    /**
     * Tries to find field instruction (in other words, {@code X a = getXSomewhere(); a.getFoo(); })
     *
     * @param insnstructions Instructions list
     * @param fieldOpcode Field opcode, see {@link FieldOpcode}
     * @param owner Field owner class (in internal class name format, like {@code foo/bar/Baz})
     * @param fieldName Field name
     * @param fieldType Field type in descriptor format, like {@code Ljava/lang/String;}
     * @return First found instruction or null, if not found
     */
    @Nullable
    public static FieldInsnNode findFieldInstruction(@NotNull InsnList insnstructions,
                                                     @NotNull FieldOpcode fieldOpcode,
                                                     @NotNull String owner,
                                                     @NotNull String fieldName,
                                                     @NotNull String fieldType) {
        return firstOrNull(findFieldInstructions(insnstructions, fieldOpcode, owner, fieldName, fieldType));
    }

    /**
     * Reroutes field getter to static method
     *
     * Note: Target field getter must be static as well for now!
     *
     * @param instructions Instructions list
     * @param fieldInsn Field instruction
     * @param owner Reroute method owner class (in internal class name format, like {@code foo/bar/Baz})
     * @param name Reroute method name
     * @param desc Reroute method descriptor
     */
    public static void rerouteFieldGetter(@NotNull InsnList instructions,
                                   @NotNull FieldInsnNode fieldInsn,
                                   @NotNull String owner,
                                   @NotNull String name,
                                   @NotNull String desc) {
        if(!instructions.contains(fieldInsn))
            throw new IllegalStateException("Given instructions list does not contain provided instruction!");

        // TODO: :(
        if(fieldInsn.getOpcode() != Opcodes.GETSTATIC)
            throw new IllegalStateException("Only static field getter rerouting is supported for now!");

        if(fieldInsn.getOpcode() != Opcodes.GETSTATIC && fieldInsn.getOpcode() != Opcodes.GETFIELD)
            throw new IllegalStateException("Instruction opcode must be GET or GETSTATIC!");

        Type[] methodParams = Type.getArgumentTypes(desc);
        Type methodReturn = Type.getReturnType(desc);

        if(methodParams.length > 0 || methodReturn.equals(Type.VOID_TYPE))
            throw new IllegalStateException("Target method must not take any arguments and not return void!");

        // TODO: Check target return type compatibility
        // TODO: Check if target method is accessible
        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
        instructions.set(fieldInsn, newInsn);
    }

    /**
     * Reroutes filed getter to static method
     *
     * @param instructions Instructions list
     * @param fieldInsn Field instruction
     * @param target Target method
     */
    public static void rerouteFieldGetter(@NotNull InsnList instructions,
                                   @NotNull FieldInsnNode fieldInsn,
                                   @NotNull Method target) {
        int modifiers = target.getModifiers();
        if(!Modifier.isStatic(modifiers))
            throw new IllegalStateException("Only static methods are supported for rerouting!");
        String owner = target.getDeclaringClass().getName().replace('.', '/');
        String desc = Type.getMethodDescriptor(target);
        rerouteFieldGetter(instructions, fieldInsn, owner, target.getName(), desc);
    }

    /**
     * Reroutes field setter to static method
     *
     * Note: Target field setter must be static as well for now!
     *
     * @param instructions Instructions list
     * @param fieldInsn Field instruction
     * @param owner Reroute method owner class (in internal class name format, like {@code foo/bar/Baz})
     * @param name Reroute method name
     * @param desc Reroute method descriptor
     */
    public static void rerouteFieldSetter(@NotNull InsnList instructions,
                                          @NotNull FieldInsnNode fieldInsn,
                                          @NotNull String owner,
                                          @NotNull String name,
                                          @NotNull String desc) {
        if(!instructions.contains(fieldInsn))
            throw new IllegalStateException("Given instructions list does not contain provided instruction!");

        // TODO: :(
        if(fieldInsn.getOpcode() != Opcodes.PUTSTATIC)
            throw new IllegalStateException("Only static field setter rerouting is supported for now!");

        if(fieldInsn.getOpcode() != Opcodes.PUTSTATIC && fieldInsn.getOpcode() != Opcodes.PUTFIELD)
            throw new IllegalStateException("Instruction opcode must be PUT or PUTSTATIC!");

        Type[] methodParams = Type.getArgumentTypes(desc);
        Type methodReturn = Type.getReturnType(desc);

        if(methodParams.length != 1 || !methodReturn.equals(Type.VOID_TYPE))
            throw new IllegalStateException("Target method must take exactly one argument and return void!");

        // TODO: Check target parameter type compatibility
        // TODO: Check if target method is accessible
        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
        instructions.set(fieldInsn, newInsn);
    }

    /**
     * Reroutes filed setter to static method
     *
     * @param instructions Instructions list
     * @param fieldInsn Field instruction
     * @param target Target method
     */
    public static void rerouteFieldSetter(@NotNull InsnList instructions,
                                          @NotNull FieldInsnNode fieldInsn,
                                          @NotNull Method target) {
        int modifiers = target.getModifiers();
        if(!Modifier.isStatic(modifiers))
            throw new IllegalStateException("Only static methods are supported for rerouting!");
        String owner = target.getDeclaringClass().getName().replace('.', '/');
        String desc = Type.getMethodDescriptor(target);
        rerouteFieldSetter(instructions, fieldInsn, owner, target.getName(), desc);
    }

    /**
     * Method opcodes
     */
    public enum MethodOpcode {
        /**
         * INVOKEVIRTUAL, instance method invocation like {@code String instance = "a"; a.length();}
         */
        VIRTUAL(Opcodes.INVOKEVIRTUAL),

        /**
         * INVOKESTATIC, static method invocation like {@code System.gc()}
         */
        STATIC(Opcodes.INVOKESTATIC),

        /**
         * INVOKESPECIAL, private and superclass (like {@code super.toString()}) method invocations
         */
        SPECIAL(Opcodes.INVOKESPECIAL),

        /**
         * INVOKEINTERFACE, for methods which implement interface (thus method is forced to be public)
         */
        INTERFACE(Opcodes.INVOKEINTERFACE)

        ;


        private final int opcode;

        MethodOpcode(int opcode) {
            this.opcode = opcode;
        }
    }

    /**
     * Field opcodes
     */
    public enum FieldOpcode {
        /**
         * GET, instance field getter like {@code int opcode = FieldOpcode.GET.opcode}
         */
        GET(Opcodes.GETFIELD),

        /**
         * PUT, instance field setter like {@code FieldOpcode.SET.opcode = Opcodes.PUTFIELD} (just an example, not meant to work)
         */
        PUT(Opcodes.PUTFIELD),

        /**
         * GETSTATIC, static field getter like {@code String a = Attributes.Name.MANIFEST_VERSION;}
         */
        GETSTATIC(Opcodes.GETSTATIC),

        /**
         * PUTSTATIC, static field setter like {@code Attributes.Name.MANIFEST_VERSION = "Manifest-Version";}
         */
        PUTSTATIC(Opcodes.PUTSTATIC)

        ;

        private final int opcode;

        FieldOpcode(int opcode) {
            this.opcode = opcode;
        }
    }
}
