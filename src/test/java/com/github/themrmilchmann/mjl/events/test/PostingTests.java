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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.themrmilchmann.mjl.events.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class PostingTests {

    @Test
    public void testSingleSubscriberPost() {
        EventBus<Object> bus = EventBus.builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(DefaultExecutors.directExecutor())
            .build();

        SubscriberHandle handle = bus.subscribe(TestEvent.TestCompletableFutureEvent.class, event -> event.getFuture().complete(null));
        assertNotNull(handle);

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();

        handle.unsubscribe();
    }

    @Test
    public void testAssignableResolutionPost() throws InterruptedException {
        EventBus<Object> bus = EventBus.builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(DefaultExecutors.directExecutor())
            .build();

        SubscriberHandle handle = bus.subscribe(TestEvent.TestCountdownLatchEvent.class, event -> event.getLatch().countDown());
        assertNotNull(handle);

        CountDownLatch latch = new CountDownLatch(2);
        bus.post(new TestEvent());
        bus.post(new TestEvent.TestCountdownLatchEvent(latch));
        bus.post(new TestEvent.TestCountdownLatchEvent2(latch));
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        handle.unsubscribe();
    }

    @Test
    public void testDeadPost() {
        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);

        EventBus<TestDeadEvent> bus = EventBus.builder(TestDeadEvent.class)
            .setDeadEventHandler(it -> it.getFuture().complete(null))
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(DefaultExecutors.directExecutor())
            .build();

        bus.post(new TestDeadEvent(future));
        future.join();
    }

}