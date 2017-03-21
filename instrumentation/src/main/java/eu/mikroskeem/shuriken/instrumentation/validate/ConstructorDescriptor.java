package eu.mikroskeem.shuriken.instrumentation.validate;

import lombok.Getter;
import org.jetbrains.annotations.Contract;


/**
 * Constructor descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@Getter
public class ConstructorDescriptor {
    private final Class[] arguments;
    private ConstructorDescriptor(Class... arguments){
        this.arguments = arguments;
    }

    @Contract("_ -> !null")
    public static ConstructorDescriptor of(Class... arguments){
        return new ConstructorDescriptor(arguments);
    }
}
