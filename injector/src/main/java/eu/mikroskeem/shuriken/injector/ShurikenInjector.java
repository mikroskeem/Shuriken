package eu.mikroskeem.shuriken.injector;

import eu.mikroskeem.shuriken.common.SneakyThrow;
import eu.mikroskeem.shuriken.reflect.Reflect;
import eu.mikroskeem.shuriken.reflect.wrappers.FieldWrapper;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

/**
 * @author Mark Vainomaa
 */
@RequiredArgsConstructor
public class ShurikenInjector implements Injector {
    private final Binder binder;

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
    public static Injector createInjector(Binder.Builder builder){
        Binder binder = new Binder();
        builder.configure(binder);
        return new ShurikenInjector(binder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getInstance(Class<T> clazz) {
        try {
            return injectMembers(Reflect.wrapClass(clazz).construct().getClassInstance());
        } catch (Exception e){
            SneakyThrow.throwException(e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T injectMembers(T instance) {
        try {
            Reflect.wrapInstance(instance).getFields().forEach(this::injectField);
            return instance;
        } catch (Exception e){
            SneakyThrow.throwException(e);
        }
        return null;
    }

    /* Get binding for class */
    @SuppressWarnings("unchecked")
    private <T> Binder.Target<T> getTarget(Class<T> clazz){
        return (Binder.Target<T>)binder.getBindings().get(clazz);
    }

    /* Inject field */
    private <T> void injectField(FieldWrapper<T> field){
        try {
            if(field.getAnnotation(Inject.class).isPresent()) {
                Binder.Target<T> target;
                if((target = getTarget(field.getType())) != null){
                    field.write(target.getInstance());
                } else {
                    throw new IllegalStateException("There are no registered bindings for class: " + field.getType());
                }
            }
        } catch (Exception e){
            SneakyThrow.throwException(e);
        }
    }
}
