package eu.mikroskeem.shuriken.injector;

import eu.mikroskeem.shuriken.common.Ensure;

import java.lang.annotation.Annotation;

/**
 * Implementation of {@link javax.inject.Named} used to define
 * named annotations easily
 *
 * @author Mark Vainomaa
 */
public class Named implements javax.inject.Named {
    private final String value;
    Named(String value) {
        this.value = Ensure.notNull(value, "Value shouldn't be null!");
    }

    public static Named as(String name) {
        return new Named(name);
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return javax.inject.Named.class;
    }

    @Override
    public String toString() {
        return "@"+getClass().getName()+"(value="+value+")";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof javax.inject.Named && ((javax.inject.Named) obj).value().equals(this.value());
    }
}
