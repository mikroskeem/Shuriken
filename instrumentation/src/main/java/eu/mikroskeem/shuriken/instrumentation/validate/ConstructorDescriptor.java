package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Constructor descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@Getter
public class ConstructorDescriptor {
    private final Class[] arguments;
    private ConstructorDescriptor(Class<?>... arguments){
        this.arguments = arguments;
    }

    @Contract("_ -> !null")
    public static ConstructorDescriptor ofWrapped(ClassWrapper<?>... classes){
        Class[] args = Stream.of(classes).map(ClassWrapper::getWrappedClass)
                .collect(Collectors.toList()).toArray(new Class[0]);
        return of(args);
    }

    @Contract("_ -> !null")
    public static ConstructorDescriptor of(Class<?>... arguments){
        return new ConstructorDescriptor(arguments);
    }
}
