package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to mark method name/type (optional)
 *
 * @author Mark Vainomaa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TargetMethod {
    /*/*
     * Target method name
     *
     * Method description is taken from interface method, unless {@link TargetMethod#desc()}
     * is preset
     */

    /**
     * Target method name
     *
     * Method description is taken from interface method
     */
    String value() default "";

    /*/*
     * Target method description, like <pre>Ljava/lang/String;</pre>
     *
     * Useful for non-public classes. Use {@link Object} in place of given parameters in
     * interface method if defined.
     * /
    String desc() default "";*/
}
