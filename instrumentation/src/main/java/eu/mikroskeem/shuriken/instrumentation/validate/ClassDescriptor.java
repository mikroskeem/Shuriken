package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public class ClassDescriptor {
    private final Class<?> clazz;
    private final Class<?>[] extendingClasses;
    private ClassDescriptor(Class<?> clazz, Class<?>... arguments) {
        this.clazz = Ensure.notNull(clazz, "Class shouldn't be null");
        this.extendingClasses = arguments;
    }

    @NotNull
    public Class<?> getDescribedClass() {
        return clazz;
    }

    @NotNull
    public Class<?>[] getExtendingClasses() {
        return extendingClasses;
    }

    @NotNull
    @Contract("_, _ -> !null")
    public static ClassDescriptor ofWrapped(ClassWrapper<?> cw, ClassWrapper<?>... cws) {
        return ofWrapped(Ensure.notNull(cw, "ClassWrapper shouldn't be null!").getWrappedClass(), cws);
    }

    @NotNull
    @Contract("_, _ -> !null")
    public static ClassDescriptor ofWrapped(ClassWrapper<?> cw, Class<?>... classes) {
        return of(Ensure.notNull(cw, "ClassWrapper shouldn't be null!").getWrappedClass(), classes);
    }

    @NotNull
    @Contract("_, _ -> !null")
    public static ClassDescriptor ofWrapped(Class<?> clazz, ClassWrapper<?>... cws) {
        Class[] classes = Stream.of(cws).map(ClassWrapper::getWrappedClass)
                .collect(Collectors.toList()).toArray(new Class[cws.length]);
        return of(Ensure.notNull(clazz, "Class shouldn't be null!"), classes);
    }

    @NotNull
    @Contract("null, null -> fail")
    public static ClassDescriptor of(Class<?> clazz, Class<?>... classes) {
        return new ClassDescriptor(Ensure.notNull(clazz, "Class shouldn't be null!"), classes);
    }
}
