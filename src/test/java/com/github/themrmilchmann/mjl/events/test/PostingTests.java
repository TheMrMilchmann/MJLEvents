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

import com.github.themrmilchmann.mjl.events.*;
import com.github.themrmilchmann.mjl.events.util.concurrent.MJLExecutors;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

@Test
public final class PostingTests {

    public void testSingleSubscriberPost() {
        EventBus bus = new EventBus.Builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(MJLExecutors.directExecutor())
            .build();
        bus.register(this, MethodHandles.lookup());

        CompletableFuture future = new CompletableFuture().orTimeout(1, TimeUnit.SECONDS);
        bus.post(new TestEvent.TestCompletableFutureEvent(future));
        future.join();
    }

    public void testAssignableResolutionPost() throws InterruptedException {
        EventBus bus = new EventBus.Builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(MJLExecutors.directExecutor())
            .build();
        bus.register(this, MethodHandles.lookup());

        CountDownLatch latch = new CountDownLatch(2);
        bus.post(new TestEvent.TestCountdownLatchEvent(latch));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    public void testDeadPost() {
        EventBus bus = new EventBus.Builder()
            .setDispatcher(EventDispatcher.directDispatcher())
            .setExecutor(MJLExecutors.directExecutor())
            .build();
        bus.register(this, MethodHandles.lookup());

        CompletableFuture future = new CompletableFuture().orTimeout(1, TimeUnit.SECONDS);
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

    @SuppressWarnings("unused")
    @EventSubscriber
    private void instanceDeadSubscriber(DeadEvent event) {
        Event source = event.getEvent();

        if (source instanceof TestDeadEvent) {
            ((TestDeadEvent) source).getFuture().complete(null);
        }
    }

}