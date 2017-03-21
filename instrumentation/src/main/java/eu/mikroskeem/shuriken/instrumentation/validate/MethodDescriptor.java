package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static MethodDescriptor ofWrapped(String methodName, ClassWrapper<?> returnType, ClassWrapper<?>... arguments){
        return ofWrapped(methodName, returnType.getWrappedClass(), arguments);
    }

    @Contract("_, _, _ -> !null")
    public static MethodDescriptor ofWrapped(String methodName, ClassWrapper<?> returnType, Class<?>... arguments){
        return of(methodName, returnType.getWrappedClass(), arguments);
    }

    @Contract("_, _, _ -> !null")
    public static MethodDescriptor ofWrapped(String methodName, Class<?> returnType, ClassWrapper<?>... arguments){
        Class[] args = Stream.of(arguments).map(ClassWrapper::getWrappedClass)
                .collect(Collectors.toList()).toArray(new Class[0]);
        return of(methodName, returnType, args);
    }

    @Contract("_, _, _ -> !null")
    public static MethodDescriptor of(String methodName, Class<?> returnType, Class<?>... arguments){
        return new MethodDescriptor(methodName, returnType, arguments);
    }
}
