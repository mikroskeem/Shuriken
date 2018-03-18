package eu.mikroskeem.shuriken.instrumentation;

import org.jetbrains.annotations.Contract;
import org.objectweb.asm.Type;

import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Build method descriptor for OW2 ASM
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public final class Descriptor {
    private Descriptor() {}
    private String accepts = "";
    private String returns = "V";
    private final String finalString = "(%s)%s";

    /**
     * Get new descriptor builder instance
     *
     * @return {@link Descriptor} instance
     */
    @Contract(" -> !null")
    public static Descriptor newDescriptor() {
        return new Descriptor();
    }

    /**
     * Build method accepts part
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor accepts(String... arguments) {
        StringBuilder builder = new StringBuilder();
        for (String argument : arguments) builder.append(argument);
        this.accepts = builder.toString();
        return this;
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor accepts(Class<?>... arguments) {
        return accepts(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor accepts(Type... arguments) {
        return accepts(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor returns(String... arguments) {
        StringBuilder builder = new StringBuilder();
        for (String argument : arguments) builder.append(argument);
        this.returns = builder.toString();
        return this;
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor returns(Class<?>... arguments) {
        return returns(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    public Descriptor returns(Type... arguments) {
        return returns(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Builds descriptor
     *
     * @return Descriptor string
     */
    public String build() {
        return String.format(finalString, accepts, returns);
    }

    /**
     * Builds descriptor
     *
     * @return Descriptor string
     */
    @Override
    public String toString() {
        return build();
    }
}
