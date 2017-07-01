package eu.mikroskeem.shuriken.instrumentation.methodreflector;

import eu.mikroskeem.shuriken.common.Ensure;
import eu.mikroskeem.shuriken.common.data.Pair;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * @author Mark Vainomaa
 */
public final class MethodReflector<T> {
    private final static Map<Pair<ClassWrapper<?>, Class<?>>, MethodReflector<?>> methodReflectors = new WeakHashMap<>();
    private final static Map<String, String> replacements = new HashMap<>();
    private final static MethodReflectorFactory factory = new MethodReflectorFactory();
    public static boolean DEBUG = false;
    private MethodReflector(ClassWrapper<?> target, Class<T> itf) {
        this.clazz = target;
        this.itf = itf;
        this.interfaceImpl = factory.generateReflector(target, itf, replacements);
    }

    private final ClassWrapper<?> clazz;
    private final Class<T> itf;
    private final T interfaceImpl;

    /**
     * Generates new MethodReflector for target class
     *
     * @param targetClass Target class to target
     * @param itf Interface which calls will be delegated to target class
     * @param <T> Interface type
     * @return Instance of {@link MethodReflector}
     */
    @SuppressWarnings("unchecked")
    @NotNull
    @Contract("null, null -> fail")
    public static <T> MethodReflector<T> newInstance(ClassWrapper<?> targetClass, Class<T> itf) {
        Ensure.notNull(targetClass, "Target class shouldn't be null!");
        Ensure.notNull(itf, "Interface class shoudln't be null!");
        Ensure.ensureCondition(Modifier.isInterface(itf.getModifiers()), "Interface class should be interface!");
        Ensure.ensureCondition(Modifier.isPublic(itf.getModifiers()), "Interface should be public!");
        return (MethodReflector<T>) methodReflectors.computeIfAbsent(new Pair<>(targetClass, itf), k ->
            new MethodReflector<>(targetClass, itf)
        );
    }

    /**
     * Gets annotation placeholder replacements
     *
     * @return Map of placeholders and values
     */
    @NotNull
    @Contract(pure = true)
    public static Map<String, String> getAnnotationReplacements() {
        return Collections.unmodifiableMap(replacements);
    }

    /**
     * Registers new annotation placeholder replacement
     *
     * @param placeholder Placeholder name, like <pre>nms_ver</pre>
     * @param replacement Replacement string, like <pre>v1_12_R1</pre>
     */
    @Contract("null, null -> fail")
    public static void registerAnnotationReplacement(String placeholder, String replacement) {
        Ensure.notNull(placeholder, "Placeholder shouldn't be null!");
        Ensure.notNull(replacement, "Replacement shouldn't be null!");
        replacements.compute(placeholder, (k,v) -> replacement);
    }

    /**
     * Unregisters placeholder replacement
     *
     * @param placeholder Placeholder name, like <pre>nms_ver</pre>
     */
    @Contract("null -> fail")
    public static void unregisterAnnotationReplacement(String placeholder) {
        replacements.compute(Ensure.notNull(placeholder, "Placeholder shouldn't be null!"), (k,v) -> null);
    }

    /**
     * Gets interface class
     *
     * @return Interface class
     */
    @Contract(pure = true)
    @NotNull
    public Class<T> getInterface() {
        return itf;
    }

    /**
     * Gets target class
     *
     * @return Target class
     */
    @Contract(pure = true)
    @NotNull
    public ClassWrapper<?> getTargetClass() {
        return clazz;
    }

    /**
     * Gets reflector implementation (generated from interface)
     *
     * @return Reflector implementation
     */
    @Contract(pure = true)
    @NotNull
    public T getReflector() {
        return interfaceImpl;
    }

    @Override
    public String toString() {
        return "MethodReflector{target=" + clazz +", interface=" + itf + "}";
    }
}
