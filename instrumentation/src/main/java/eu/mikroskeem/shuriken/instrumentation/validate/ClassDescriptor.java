package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.reflect.Reflect;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

import static eu.mikroskeem.shuriken.common.Ensure.notNull;

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

    @Contract("_, _ -> !null; null, _ -> fail")
    public static ClassDescriptor of(@NonNull Class<?> clazz, Class<?>... classes){
        return new ClassDescriptor(clazz, classes);
    }

    @Contract("null, _ -> fail")
    public static ClassDescriptor of(String clazzName, Class<?>... classes){
        Class<?> clazz = notNull(
                Reflect.getClass(clazzName).orElse(null),
                "Failed to find class " + clazzName
        ).getWrappedClass();
        return new ClassDescriptor(clazz, classes);
    }
}
