package eu.mikroskeem.shuriken.instrumentation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
    /** Default method descriptor */
    public final static String DEFAULT = "()V";

    private String accepts = "";
    private String returns = "V";
    private final String finalString = "(%s)%s";

    /**
     * Get new descriptor builder instance
     *
     * @return {@link Descriptor} instance
     * @deprecated Use constructor instead
     */
    @Deprecated
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
    @NotNull
    public Descriptor accepts(@NotNull String... arguments) {
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
    @NotNull
    public Descriptor accepts(@NotNull Class<?>... arguments) {
        return accepts(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    @NotNull
    public Descriptor accepts(@NotNull Type... arguments) {
        return accepts(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    @NotNull
    public Descriptor returns(@NotNull String... arguments) {
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
    @NotNull
    public Descriptor returns(@NotNull Class<?>... arguments) {
        return returns(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Build method returns part (default is primitive {@link Void})
     *
     * @param arguments Types what method accepts
     * @return this {@link Descriptor} instance
     */
    @NotNull
    public Descriptor returns(@NotNull Type... arguments) {
        return returns(Stream.of(arguments).map(Type::getDescriptor).toArray(String[]::new));
    }

    /**
     * Builds descriptor
     *
     * @return Descriptor string
     */
    @NotNull
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
