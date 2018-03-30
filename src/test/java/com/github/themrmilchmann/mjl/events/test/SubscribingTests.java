/*
 * Copyright 2018 Leon Linhart
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
package com.github.themrmilchmann.mjl.events.test;

import com.github.themrmilchmann.mjl.events.Event;
import com.github.themrmilchmann.mjl.events.EventBus;
import com.github.themrmilchmann.mjl.events.EventSubscriber;
import org.testng.annotations.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Test
public final class SubscribingTests {

    @Retention(RetentionPolicy.RUNTIME)
    @interface PublicSubscriber {}

    @Test(enabled = false)
    @PublicSubscriber
    public void instanceSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test(enabled = false)
    @PublicSubscriber
    public static void staticSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test
    public void testPublicInstanceSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(PublicSubscriber.class)
            .build();
        bus.register(this, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPublicStaticSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(PublicSubscriber.class)
            .build();
        bus.register(SubscribingTests.class, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPublicAnonymousSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(PublicSubscriber.class)
            .build();

        bus.register(new Object() {

            @SuppressWarnings("unused")
            @PublicSubscriber
            public void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ProtectedSubscriber {}

    @Test(enabled = false)
    @ProtectedSubscriber
    public void proInstanceSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test(enabled = false)
    @ProtectedSubscriber
    protected static void proStaticSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test
    public void testProtectedInstanceSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(ProtectedSubscriber.class)
            .build();
        bus.register(this, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testProtectedStaticSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(ProtectedSubscriber.class)
            .build();
        bus.register(SubscribingTests.class, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testProtectedAnonymousSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(ProtectedSubscriber.class)
            .build();

        bus.register(new Object() {

            @SuppressWarnings("unused")
            @ProtectedSubscriber
            protected void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface PrivateSubscriber {}

    @SuppressWarnings("unused")
    @Test(enabled = false)
    @PrivateSubscriber
    private void prvInstanceSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @SuppressWarnings("unused")
    @Test(enabled = false)
    @PrivateSubscriber
    private static void prvStaticSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test
    public void testPrivateInstanceSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(PrivateSubscriber.class)
            .build();
        bus.register(this, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPrivateStaticSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(PrivateSubscriber.class)
            .build();
        bus.register(SubscribingTests.class, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrivateAnonymousSubscriber() {
        EventBus bus = new EventBus.Builder()
            .setSubscriberMarker(PrivateSubscriber.class)
            .build();

        bus.register(new Object() {

            @SuppressWarnings("unused")
            @PrivateSubscriber
            private void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIllegalParameterCountSubscriber() {
        EventBus bus = new EventBus.Builder().build();

        bus.register(new Object() {

            @SuppressWarnings("unused")
            @EventSubscriber
            public void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event, Event event2) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIllegalParameterTypeSubscriber() {
        EventBus bus = new EventBus.Builder().build();

        bus.register(new Object() {

            @SuppressWarnings("unused")
            @EventSubscriber
            public void anonymousSubscriber(Object event) {
                ((TestEvent.TestCompletableFutureEvent) event).getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = new CompletableFuture<>().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

}