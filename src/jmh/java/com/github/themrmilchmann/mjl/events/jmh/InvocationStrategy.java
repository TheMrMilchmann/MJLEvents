/*
 * Copyright 2018-2019 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.themrmilchmann.mjl.events.jmh;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class InvocationStrategy {

    /*
     * Benchmark                          (subCount)   Mode  Cnt          Score         Error  Units
     * InvocationStrategy.mh_Invoke                1  thrpt    5   98352622.658 ± 1241839.229  ops/s
     * InvocationStrategy.mh_Invoke                8  thrpt    5   14917982.446 ±  165254.374  ops/s
     * InvocationStrategy.mh_Invoke              128  thrpt    5     564037.673 ±    3782.659  ops/s
     * InvocationStrategy.mh_Invoke             1024  thrpt    5      21463.659 ±     125.429  ops/s
     * InvocationStrategy.mh_InvokeExact           1  thrpt    5  101710408.419 ±  600526.832  ops/s
     * InvocationStrategy.mh_InvokeExact           8  thrpt    5   14215572.937 ±  712361.123  ops/s
     * InvocationStrategy.mh_InvokeExact         128  thrpt    5     587899.097 ±    6940.302  ops/s
     * InvocationStrategy.mh_InvokeExact        1024  thrpt    5      20965.058 ±      75.972  ops/s
     * InvocationStrategy.reflect_Invoke           1  thrpt    5   69683414.361 ±  441456.730  ops/s
     * InvocationStrategy.reflect_Invoke           8  thrpt    5   10413506.562 ±   53938.708  ops/s
     * InvocationStrategy.reflect_Invoke         128  thrpt    5     641747.854 ±    6172.045  ops/s
     * InvocationStrategy.reflect_Invoke        1024  thrpt    5      70544.982 ±     305.504  ops/s
     *
     * Win 10 x64 OpenJDK 11.0.1b13
     *
     * NOTE:
     * The JMH Gradle plugin sometimes seems to fail for no obvious reason. Running with `--debug` once seems to fix it.
     */

    public List<Subscriber> subscribers;

    @Param({ "1", "8", "128", "1024" })
    public int subCount;

    @Setup
    public void init() throws Throwable {
        subscribers = new ArrayList<>();

        for (int i = 0; i < this.subCount; i++) {
            TestSub testSub = new TestSub();
            Method method = TestSub.class.getMethod("doSomething", Blackhole.class);
            subscribers.add(new Subscriber(testSub, method));
        }
    }

    @Benchmark
    public void reflect_Invoke(Blackhole bh) throws Throwable {
        for (Subscriber subscriber : this.subscribers) {
            subscriber.method.invoke(subscriber.instance, bh);
        }
    }

    @Benchmark
    public void mh_Invoke(Blackhole bh) throws Throwable {
        for (Subscriber subscriber : this.subscribers) {
            subscriber.handle.invoke(bh);
        }
    }

    @Benchmark
    public void mh_InvokeExact(Blackhole bh) throws Throwable {
        for (Subscriber subscriber : this.subscribers) {
            subscriber.handle.invokeExact(bh);
        }
    }

    public static final class Subscriber {

        private final Object instance;
        private final Method method;
        private final MethodHandle handle;

        private Subscriber(Object instance, Method method) throws Throwable {
            this.instance = instance;
            this.method = method;
            this.handle = MethodHandles.lookup().unreflect(method).bindTo(instance);
        }

    }

    public static final class TestSub {

        public void doSomething(Blackhole bh) {
            bh.consume(Boolean.TRUE);
        }

    }

}