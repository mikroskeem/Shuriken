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
