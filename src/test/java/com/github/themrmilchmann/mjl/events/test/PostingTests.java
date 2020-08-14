/*
 * Copyright 2018-2019 Leon Linhart
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

import com.github.themrmilchmann.mjl.events.*;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

@Test
public final class PostingTests {

    public void testSingleSubscriberPost() {
        EventBus<Object> bus = EventBus.builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(DefaultExecutors.directExecutor())
            .build();
        bus.register(this, MethodHandles.lookup());

        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    public void testAssignableResolutionPost() throws InterruptedException {
        EventBus<Object> bus = EventBus.builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(DefaultExecutors.directExecutor())
            .build();
        bus.register(this, MethodHandles.lookup());

        CountDownLatch latch = new CountDownLatch(2);
        bus.post(new TestEvent.TestCountdownLatchEvent(latch));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    public void testDeadPost() {
        CompletableFuture<?> future = Util.timeoutAfter(1, TimeUnit.SECONDS);

        EventBus<TestDeadEvent> bus = EventBus.builder(TestDeadEvent.class)
            .setDeadEventHandler(it -> it.getFuture().complete(null))
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(DefaultExecutors.directExecutor())
            .build();
        bus.register(this, MethodHandles.lookup());

        bus.post(new TestDeadEvent(future));
        future.join();
    }

    @Test(enabled = false)
    @EventSubscriber
    public void instanceSubscriber(TestEvent event) {
        if (event instanceof TestEvent.TestCountdownLatchEvent) {
            ((TestEvent.TestCountdownLatchEvent) event).getLatch().countDown();
        }
    }

    @SuppressWarnings("unused")
    @EventSubscriber
    private void instanceCFSubscriber(TestEvent.TestCompletableFutureEvent event) {
        event.getFuture().complete(null);
    }

    @SuppressWarnings("unused")
    @EventSubscriber
    private void instanceCLSubscriber(TestEvent.TestCountdownLatchEvent event) {
        event.getLatch().countDown();
    }

}