package eu.mikroskeem.shuriken.instrumentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
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
    private String returns = "V";
    private final String finalString = "(%s)%s";

    /**
     * Get new descriptor builder instance
     *
     * @return {@link Descriptor} instance
     */
    @Contract(" -> !null")
    public static Descriptor newDescriptor(){
        return new Descriptor();
    }

    /**
     * Build method accepts part
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor accepts(Class<?>... arguments){
        StringBuilder builder = new StringBuilder();
        for (Class<?> argument : arguments) {
            builder.append(Type.getDescriptor(argument));
        }
        this.accepts = builder.toString();
        return this;
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor returns(Class<?>... arguments){
        StringBuilder builder = new StringBuilder();
        for (Class<?> argument : arguments) {
            builder.append(Type.getDescriptor(argument));
        }
        this.returns = builder.toString();
        return this;
    }

    /**
     * Builds descriptor
     *
     * @return Descriptor string
     */
    @Override
    public String toString(){
        return String.format(finalString, accepts, returns);
    }
}
