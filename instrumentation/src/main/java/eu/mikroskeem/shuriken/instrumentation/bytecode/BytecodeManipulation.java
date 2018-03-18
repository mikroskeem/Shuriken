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
import java.util.function.Predicate;

/**
 * @author Mark Vainomaa
 */
public final class BytecodeManipulation {
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> T findInstruction(@NotNull InsnList instructions,
                                                                 @NotNull Class<T> instructionType,
                                                                 @NotNull Predicate<T> predicate) {
        return Arrays.stream(instructions.toArray())
                .filter(i -> i.getClass() == instructionType)
                .map(instructionType::cast)
                .filter(predicate)
                .findFirst().orElse(null);
    }

    @Nullable
    public static TypeInsnNode findTypeInstantiation(@NotNull InsnList insnstructions, @NotNull String type) {
        return findInstruction(insnstructions, TypeInsnNode.class, i -> i.getOpcode() == Opcodes.NEW && type.equals(i.desc));
    }

    public static void rerouteTypeInstantiation(@NotNull InsnList instructions,
                                                @NotNull TypeInsnNode typeInsnNode,
                                                @NotNull String owner,
                                                @NotNull String name,
                                                @NotNull String desc,
                                                boolean invokeInterface) {
        if(!instructions.contains(typeInsnNode))
            throw new IllegalStateException("Given instructions list does not contain provided instruction!");

        Type[] originalDesc = Type.getArgumentTypes(typeInsnNode.desc);
        Type[] targetDesc = Type.getArgumentTypes(desc);

        if(originalDesc.length != targetDesc.length)
            throw new IllegalStateException("Target method's parameter count must match with constructor!");

        // TODO: keep this only static?
        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, invokeInterface);
        instructions.set(typeInsnNode, newInsn);
    }

    @Nullable
    public static MethodInsnNode findMethodInvocation(@NotNull InsnList insnstructions,
                                                      @NotNull MethodOpcode methodOpcode,
                                                      @NotNull String owner,
                                                      @NotNull String name,
                                                      @NotNull String desc) {
        return findInstruction(insnstructions, MethodInsnNode.class, m -> m.getOpcode() == methodOpcode.opcode &&
                owner.equals(m.owner) &&
                name.equals(m.name) &&
                desc.equals(m.desc) &&
                (methodOpcode.opcode != Opcodes.INVOKEINTERFACE || m.itf)
        );
    }

    public static void rerouteMethodInvocation(@NotNull InsnList instructions,
                                               @NotNull MethodInsnNode methodInsn,
                                               @NotNull String owner,
                                               @NotNull String name,
                                               @NotNull String desc,
                                               boolean invokeInterface) {
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
        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, invokeInterface);
        instructions.set(methodInsn, newInsn);
    }

    public static void rerouteMethodInvocation(@NotNull InsnList instructions,
                                               @NotNull MethodInsnNode methodInsn,
                                               @NotNull Method target) {
        int modifiers = target.getModifiers();
        String owner = target.getDeclaringClass().getName().replace('.', '/');
        String desc = Type.getMethodDescriptor(target);
        boolean invokeInterface = !Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && Modifier.isInterface(modifiers);
        rerouteMethodInvocation(instructions, methodInsn, owner, target.getName(), desc, invokeInterface);
    }

    @Nullable
    public static FieldInsnNode findFieldInstruction(@NotNull InsnList insnstructions,
                                                     @NotNull FieldOpcode fieldOpcode,
                                                     @NotNull String owner,
                                                     @NotNull String fieldName,
                                                     @NotNull String fieldType) {
        return findInstruction(insnstructions, FieldInsnNode.class, f -> fieldOpcode.opcode == f.getOpcode() &&
                owner.equals(f.owner) && fieldName.equals(f.name) && fieldType.equals(f.desc)
        );
    }

    public static void rerouteFieldGetter(@NotNull InsnList instructions,
                                   @NotNull FieldInsnNode fieldInsn,
                                   @NotNull String owner,
                                   @NotNull String name,
                                   @NotNull String desc,
                                   boolean invokeInterface) {
        if(!instructions.contains(fieldInsn))
            throw new IllegalStateException("Given instructions list does not contain provided instruction!");

        // TODO: :(
        if(fieldInsn.getOpcode() != Opcodes.GETSTATIC)
            throw new IllegalStateException("Only static field getter rerouting is supported for now!");

        Type[] methodParams = Type.getArgumentTypes(desc);

        if(methodParams.length > 0)
            throw new IllegalStateException("Target method must not take any arguments!");

        // TODO: Check target return type compatibility
        // TODO: Check if target method is accessible
        MethodInsnNode newInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, invokeInterface);
        instructions.set(fieldInsn, newInsn);
    }

    public static void rerouteFieldGetter(@NotNull InsnList instructions,
                                   @NotNull FieldInsnNode fieldInsn,
                                   @NotNull Method target) {
        int modifiers = target.getModifiers();
        String owner = target.getDeclaringClass().getName().replace('.', '/');
        String desc = Type.getMethodDescriptor(target);
        boolean invokeInterface = !Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && Modifier.isInterface(modifiers);
        rerouteFieldGetter(instructions, fieldInsn, owner, target.getName(), desc, invokeInterface);
    }

    public enum MethodOpcode {
        VIRTUAL(Opcodes.INVOKEVIRTUAL),
        STATIC(Opcodes.INVOKESTATIC),
        SPECIAL(Opcodes.INVOKESPECIAL),
        INTERFACE(Opcodes.INVOKEINTERFACE)

        ;


        private final int opcode;

        MethodOpcode(int opcode) {
            this.opcode = opcode;
        }
    }

    public enum FieldOpcode {
        GET(Opcodes.GETFIELD),
        PUT(Opcodes.PUTFIELD),
        GETSTATIC(Opcodes.GETSTATIC),
        PUTSTATIC(Opcodes.PUTSTATIC)

        ;

        private final int opcode;

        FieldOpcode(int opcode) {
            this.opcode = opcode;
        }
    }
}
