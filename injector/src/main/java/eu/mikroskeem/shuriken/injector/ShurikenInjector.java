package eu.mikroskeem.shuriken.injector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.reflect.FieldWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;

import javax.inject.Inject;

/**
 * @author Mark Vainomaa
 */
public class ShurikenInjector implements Injector {
    private final Binder binder;
    private ShurikenInjector(Binder binder) {
        this.binder = binder;
    }

    /**
     * Create injector out of {@link eu.mikroskeem.shuriken.injector.Binder.Builder} implementation. <br>
     * You can either use Java 8 lambdas or implement interface directly
     * <pre>
     * ShurikenInjector.createInjector(binder -&gt; {
     *      binder.bind(InterfaceA.class).to(ImplementationB.class);
     * });
     * </pre>
     *
     * @param builder {@link eu.mikroskeem.shuriken.injector.Binder.Builder} implementation
     * @return {@link Injector} instance, in this case {@link ShurikenInjector}
     */
    public static Injector createInjector(Binder.Builder builder) {
        Binder binder = new Binder();
        Ensure.notNull(builder, "Builder shouldn't be null!").configure(binder);
        return new ShurikenInjector(binder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getInstance(Class<T> clazz) {
        return injectMembers(Reflect.wrapClass(clazz).construct().getClassInstance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T injectMembers(T instance) {
        Reflect.wrapInstance(instance).getFields().forEach(this::injectField);
        return instance;
    }

    /* Get binding for class */
    @SuppressWarnings("unchecked")
    private <T> Binder.Target<T> getTarget(Class<T> clazz) {
        return (Binder.Target<T>)binder.getBindings().get(clazz);
    }

    /* Inject field */
    private <T> void injectField(FieldWrapper<T> field) {
        if(field.getAnnotation(Inject.class).isPresent()) {
            Binder.Target<T> target;
            Ensure.ensureCondition(
                    (target = getTarget(field.getType())) != null,
                    "There are no registered bindings for class: " + field.getType()
            );
            field.write(target.getInstance());
        }
    }
}
