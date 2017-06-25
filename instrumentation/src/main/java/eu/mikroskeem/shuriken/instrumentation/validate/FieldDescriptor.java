package eu.mikroskeem.shuriken.instrumentation.validate;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import org.jetbrains.annotations.Contract;

/**
 * Field descriptor
 *
 * @author Mark Vainomaa
 * @version 0.0.1
 */
public final class FieldDescriptor {
    private final String fieldName;
    private final Class<?> fieldType;
    private FieldDescriptor(String fieldName, Class<?> fieldType) {
        this.fieldName = Ensure.notNull(fieldName, "Field name shouldn't be null");
        this.fieldType = Ensure.notNull(fieldType, "Field type shouldn't be null");
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    @Contract("_, _ -> !null; null, null -> fail")
    public static FieldDescriptor of(String fieldName, ClassWrapper<?> returnType) {
        Ensure.notNull(returnType, "Field type shouldn't be null");
        return new FieldDescriptor(fieldName, returnType.getWrappedClass());
    }

    @Contract("_, _ -> !null; null, null -> fail")
    public static FieldDescriptor of(String fieldName, Class<?> returnType) {
        return new FieldDescriptor(fieldName, returnType);
    }
}
