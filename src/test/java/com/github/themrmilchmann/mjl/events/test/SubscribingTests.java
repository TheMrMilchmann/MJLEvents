/*
 * Copyright 2018-2022 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.themrmilchmann.mjl.events.test;

import com.github.themrmilchmann.mjl.events.EventBus;
import com.github.themrmilchmann.mjl.events.EventSubscriber;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public final class SubscribingTests {

    @Retention(RetentionPolicy.RUNTIME)
    @interface PublicSubscriber {}

    @PublicSubscriber
    public void instanceSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @PublicSubscriber
    public static void staticSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test
    public void testPublicInstanceSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(PublicSubscriber.class)
            .build();
        bus.subscribe(this, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPublicStaticSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(PublicSubscriber.class)
            .build();
        bus.subscribe(SubscribingTests.class, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPublicAnonymousSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(PublicSubscriber.class)
            .build();

        bus.subscribe(new Object() {

            @SuppressWarnings("unused")
            @PublicSubscriber
            public void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ProtectedSubscriber {}

    @ProtectedSubscriber
    public void proInstanceSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @SuppressWarnings({"ProtectedMemberInFinalClass", "unused"})
    @ProtectedSubscriber
    protected static void proStaticSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test
    public void testProtectedInstanceSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(ProtectedSubscriber.class)
            .build();
        bus.subscribe(this, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testProtectedStaticSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(ProtectedSubscriber.class)
            .build();
        bus.subscribe(SubscribingTests.class, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testProtectedAnonymousSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(ProtectedSubscriber.class)
            .build();

        //noinspection ProtectedMemberInFinalClass
        bus.subscribe(new Object() {

            @SuppressWarnings("unused")
            @ProtectedSubscriber
            protected void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface PrivateSubscriber {}

    @SuppressWarnings("unused")
    @PrivateSubscriber
    private void prvInstanceSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @SuppressWarnings("unused")
    @PrivateSubscriber
    private static void prvStaticSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @Test
    public void testPrivateInstanceSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(PrivateSubscriber.class)
            .build();
        bus.subscribe(this, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPrivateStaticSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(PrivateSubscriber.class)
            .build();
        bus.subscribe(SubscribingTests.class, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    @Test
    public void testPrivateAnonymousSubscriber() {
        EventBus<Object> bus = EventBus.builder()
            .setSubscriberMarker(PrivateSubscriber.class)
            .build();

        assertThrows(IllegalArgumentException.class, () -> bus.subscribe(new Object() {

            @SuppressWarnings("unused")
            @PrivateSubscriber
            private void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup()));
    }

    @Test
    public void testIllegalParameterCountSubscriber() {
        EventBus<Object> bus = EventBus.builder().build();

        assertThrows(IllegalArgumentException.class, () -> bus.subscribe(new Object() {

            @SuppressWarnings("unused")
            @EventSubscriber
            public void anonymousSubscriber(TestEvent.TestCompletableFutureEvent event, Object event2) {
                event.getFuture().complete(null);
            }

        }, MethodHandles.lookup()));
    }

}