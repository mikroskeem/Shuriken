package eu.mikroskeem.shuriken.instrumentation.validate;

import lombok.Getter;
import org.jetbrains.annotations.Contract;

/**
 * Method descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@Getter
public class MethodDescriptor {
    private final String methodName;
    private final Class<?> returnType;
    private final Class[] arguments;
    private MethodDescriptor(String methodName, Class<?> returnType, Class... arguments){
        this.methodName = methodName;
        this.returnType = returnType;
        this.arguments = arguments;
    }

    @Contract("_, _, _ -> !null")
    public static MethodDescriptor of(String methodName, Class<?> returnType, Class... arguments){
        return new MethodDescriptor(methodName, returnType, arguments);
    }
}
