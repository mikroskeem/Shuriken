package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to mark field getter
 *
 * @author Mark Vainomaa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TargetFieldGetter {
    /** Field name */
    String value();

    /*/*
     * Field type, like <pre>Ljava/lang/String;</pre>
     *
     * Useful for non-public classes. Use {@link Object} in place of return value in
     * interface method if defined.
     * /
    String type() default "";*/
}
