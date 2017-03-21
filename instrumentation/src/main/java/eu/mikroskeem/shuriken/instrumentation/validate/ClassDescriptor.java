package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@Getter
public class ClassDescriptor {
    private final Class<?> clazz;
    private final Class<?>[] extendingClasses;
    private ClassDescriptor(Class<?> clazz, Class<?>... arguments){
        this.clazz = clazz;
        this.extendingClasses = arguments;
    }

    @Contract("_, _ -> !null")
    public static ClassDescriptor ofWrapped(@NonNull ClassWrapper<?> cw, ClassWrapper<?>... cws){
        return ofWrapped(cw.getWrappedClass(), cws);
    }

    @Contract("_, _ -> !null")
    public static ClassDescriptor ofWrapped(@NonNull ClassWrapper<?> cw, Class<?>... classes){
        return of(cw.getWrappedClass(), classes);
    }

    @Contract("_, _ -> !null")
    public static ClassDescriptor ofWrapped(@NonNull Class<?> clazz, ClassWrapper<?>... cws){
        Class[] classes = Stream.of(cws).map(ClassWrapper::getWrappedClass)
                .collect(Collectors.toList()).toArray(new Class[0]);
        return of(clazz, classes);
    }

    @Contract("_, _ -> !null; null, _ -> fail")
    public static ClassDescriptor of(@NonNull Class<?> clazz, Class<?>... classes){
        return new ClassDescriptor(clazz, classes);
    }
}
