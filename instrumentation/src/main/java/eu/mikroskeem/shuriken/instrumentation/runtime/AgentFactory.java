package eu.mikroskeem.shuriken.instrumentation.runtime;

import eu.mikroskeem.shuriken.common.function.SilentConsumer;
import eu.mikroskeem.shuriken.common.function.UncheckedConsumer;
import eu.mikroskeem.shuriken.instrumentation.ClassLoaderTools;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Java agent factory
 *
 * @author Mark Vainomaa
 */
public final class AgentFactory {
    private final static Class<?> vmClass;
    private final static Method attach;
    private final static Method loadAgent;
    private final static Method detach;

    private final static List<Path> generatedAgents = new ArrayList<>();

    /**
     * Starts building a new Java agent jar
     *
     * @param agentClass Agent main class name
     * @param jarBuilder Jar builder
     * @return Agent jar path
     */
    @NotNull
    public static Path newJavaAgent(@NotNull String agentClass, @NotNull BiConsumer<Manifest, AgentJarOutputStream> jarBuilder) throws Exception {
        // Create new temporary file for agent jar
        // Note: might have an issue with systems cleaning temporary directory automatically...
        // Also it's up to user to clean up agent jar later.
        // https://stackoverflow.com/questions/28752006/alternative-to-file-deleteonexit-in-java-nio
        // https://stackoverflow.com/questions/18146637/delete-on-close-deletes-files-before-close-on-linux
        Path jarFile = Files.createTempFile("agent", ".jar");

        // Generate manifest for agent
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Agent-Class"), agentClass);
        mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");

        // Build jar contents
        try(AgentJarOutputStream os = new AgentJarOutputStream(Files.newOutputStream(jarFile), manifest, agentClass)) {
            jarBuilder.accept(manifest, os);
        }

        generatedAgents.add(jarFile);

        return jarFile;
    }

    /**
     * Starts building a new Java agent jar
     *
     * @param agentClass Agent main class name
     * @param jarBuilder Jar builder
     * @return Agent jar path
     */
    @NotNull
    public static Path newJavaAgent(@NotNull String agentClass, @NotNull Consumer<AgentJarOutputStream> jarBuilder) throws Exception {
        return newJavaAgent(agentClass, (manifest, jarOutputStream) -> jarBuilder.accept(jarOutputStream));
    }

    /**
     * Builds a new Java agent jar from {@code agentClass}
     *
     * @param agentClass Agent class
     * @return Agent jar path
     */
    @NotNull
    public static Path newJavaAgent(@NotNull Class<?> agentClass) throws Exception {
        return newJavaAgent(agentClass.getName(), (UncheckedConsumer<AgentJarOutputStream, IOException>) jarBuilder -> jarBuilder.addClassToJar(agentClass));
    }

    /**
     * Gets JVM PID
     *
     * @return JVM PID
     */
    public static long getCurrentPID() {
        String jvm = ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(jvm.substring(0, jvm.indexOf('@')));
    }

    /**
     * Attaches an agent to JVM
     *
     * @param jarPath Jar path
     */
    public static void attachAgent(@NotNull Path jarPath) throws Exception {
        Object vm = attach.invoke(null, "" + getCurrentPID());
        loadAgent.invoke(vm, jarPath.toString());
        detach.invoke(vm);
    }

    static {
        try {
            // TODO: very very very likely not compatible with Java 9 right now
            Path toolsJar = Paths.get(System.getProperty("java.home"), "..", "lib/tools.jar")
                    .toRealPath()
                    .toAbsolutePath();

            if(Files.notExists(toolsJar))
                throw new IllegalStateException("JDK is required for runtime transformations!");

            Class<?> _vmClass;
            try {
                _vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            } catch (ClassNotFoundException e) {
                ClassLoaderTools.URLClassLoaderTools uclTool = new ClassLoaderTools.URLClassLoaderTools(
                        ClassLoader.getSystemClassLoader());
                uclTool.addURL(toolsJar.toUri().toURL());
                uclTool.resetCache();
                _vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            }

            vmClass = _vmClass;
            attach = vmClass.getMethod("attach", String.class);
            loadAgent = vmClass.getMethod("loadAgent", String.class);
            detach = vmClass.getMethod("detach");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Add runtime hook to clean up generated agents
        if(!Boolean.getBoolean("shuriken.dontCleanUpAgents")) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                generatedAgents.forEach((SilentConsumer<Path>) Files::deleteIfExists);
            }, "Shuriken Generated Agent cleanup"));
        }
    }
}
