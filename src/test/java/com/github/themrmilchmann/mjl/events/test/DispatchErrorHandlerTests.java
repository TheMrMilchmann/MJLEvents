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
import com.github.themrmilchmann.mjl.events.EventDispatcher;
import com.github.themrmilchmann.mjl.events.SubscriberConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public final class DispatchErrorHandlerTests {

    @Test
    public void testBusHandler() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        EventBus<Object> bus = EventBus.builder()
            .setDispatchErrorHandler((event, subscriber, t) -> future.completeExceptionally(t))
            .build();

        bus.subscribe(Object.class, event -> { throw new RuntimeException(); });
        bus.post(new Object());

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    public void testSubscriberHandler() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        EventBus<Object> bus = EventBus.builder()
            .setDispatchErrorHandler((event, subscriber, t) -> {
                throw new RuntimeException(t);
            })
            .build();

        bus.subscribe(
            Object.class,
            event -> { throw new RuntimeException(); },
            SubscriberConfig.builder()
                .withDispatchErrorHandler(((event, subscriber, t) -> future.completeExceptionally(t)))
                .build()
        );

        bus.post(new Object());

        assertThrows(ExecutionException.class, future::get);
    }

}