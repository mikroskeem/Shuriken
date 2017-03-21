package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.reflect.wrappers.ClassWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

/**
 * Field descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FieldDescriptor {
    private final String fieldName;
    private final Class<?> fieldType;

    @Contract("_, _ -> !null")
    public static FieldDescriptor of(String fieldName, ClassWrapper<?> returnType){
        return new FieldDescriptor(fieldName, returnType.getWrappedClass());
    }

    @Contract("_, _ -> !null")
    public static FieldDescriptor of(String fieldName, Class<?> returnType){
        return new FieldDescriptor(fieldName, returnType);
    }
}
