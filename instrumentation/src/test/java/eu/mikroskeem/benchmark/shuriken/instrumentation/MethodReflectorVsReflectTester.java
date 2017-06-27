package eu.mikroskeem.benchmark.shuriken.instrumentation;

import eu.mikroskeem.benchmark.shuriken.instrumentation.testclasses.TestClass;
import eu.mikroskeem.shuriken.instrumentation.methodreflector.MethodReflector;
import eu.mikroskeem.shuriken.reflect.ClassWrapper;
import eu.mikroskeem.shuriken.reflect.Reflect;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;


/**
 * @author Mark Vainomaa
 */
public class MethodReflectorVsReflectTester {
    @Test
    public void launchBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .warmupTime(TimeValue.seconds(5))
                .warmupIterations(2)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(2)
                .threads(4)
                .forks(2)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        Runner runner = new Runner(opt);
        runner.run();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testReflect(ReflectBench reflectBench, Blackhole blackhole) {
        blackhole.consume(reflectBench.tc.invokeMethod("a", String.class));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testMethodReflectorASM(MethodReflectorASMBench asmBench, Blackhole blackhole) {
        blackhole.consume(asmBench.reflectorImpl.a());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testMethodReflectorMH(MethodReflectorMHBench mhBench, Blackhole blackhole) {
        blackhole.consume(mhBench.reflectorImpl.b());
    }

    @State(Scope.Benchmark)
    public static class ReflectBench {
        ClassWrapper<TestClass> tc;

        @Setup(Level.Trial)
        public void setup() {
            tc = Reflect.wrapInstance(new TestClass());
        }
    }

    @State(Scope.Benchmark)
    public static class MethodReflectorASMBench {
        ClassWrapper<TestClass> tc;
        MethodReflector<TestClassReflector> reflector;
        TestClassReflector reflectorImpl;

        @Setup(Level.Trial)
        public void setup() {
            tc = Reflect.wrapInstance(new TestClass());
            reflector = MethodReflector.newInstance(tc, TestClassReflector.class);
            reflectorImpl = reflector.getReflector();
        }
    }

    @State(Scope.Benchmark)
    public static class MethodReflectorMHBench {
        ClassWrapper<TestClass> tc;
        MethodReflector<TestClassReflector> reflector;
        TestClassReflector reflectorImpl;

        @Setup(Level.Trial)
        public void setup() {
            tc = Reflect.wrapInstance(new TestClass());
            reflector = MethodReflector.newInstance(tc, TestClassReflector.class);
            reflectorImpl = reflector.getReflector();
        }
    }

    public interface TestClassReflector {
        String a();
        String b();
    }
}
