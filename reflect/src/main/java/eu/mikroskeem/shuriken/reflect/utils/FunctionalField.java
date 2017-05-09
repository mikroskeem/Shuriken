package eu.mikroskeem.shuriken.reflect.utils;

import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import org.jetbrains.annotations.Contract;

/**
 * Various wrappers to aid with reflection in functional API
 *
 * @author Mark Vainomaa
 */
public class FunctionalField {
    /* Boolean field methods */
    @Contract("null -> fail")
    public static void writeTrue(FieldWrapper<Boolean> field) { field.write(true); }
    @Contract("null -> fail")
    public static void writeFalse(FieldWrapper<Boolean> field) { field.write(false); }

    /* Object based field methods */
    @Contract("null -> fail")
    public static void writeNull(FieldWrapper<?> field) {
        if(field.getField().getType().isPrimitive())
            throw new IllegalStateException("Primitive fields can't be set to null!");
        field.write(null);
    }

    /* Filter methods */
    @Contract("null -> fail")
    public static boolean notNullValue(FieldWrapper<?> field) { return field.read() != null; }

    @Contract("null -> fail")
    public static boolean notPrimitive(FieldWrapper<?> field) { return !field.getField().getType().isPrimitive(); }
}
