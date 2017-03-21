package eu.mikroskeem.shuriken.instrumentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.objectweb.asm.Type;

/**
 * Build method descriptor for OW2 ASM
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Descriptor {
    private String accepts = "";
    private String returns = "";
    private final String finalString = "(%s)%s";

    public static Descriptor newDescriptor(){
        return new Descriptor();
    }

    public Descriptor accepts(Class<?>... arguments){
        StringBuilder builder = new StringBuilder();
        for (Class<?> argument : arguments) {
            builder.append(Type.getDescriptor(argument));
        }
        this.accepts = builder.toString();
        return this;
    }

    public Descriptor returns(Class<?>... arguments){
        StringBuilder builder = new StringBuilder();
        for (Class<?> argument : arguments) {
            builder.append(Type.getDescriptor(argument));
        }
        this.returns = builder.toString();
        return this;
    }

    @Override
    public String toString(){
        return String.format(finalString, accepts, returns);
    }
}
