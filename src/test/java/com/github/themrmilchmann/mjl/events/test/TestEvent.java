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
package com.github.themrmilchmann.mjl.events.test;

import com.github.themrmilchmann.mjl.events.Event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

class TestEvent implements Event {

    static class TestCompletableFutureEvent extends TestEvent {

        private final CompletableFuture<?> future;

        TestCompletableFutureEvent(CompletableFuture<?> future) {
            this.future = future;
        }

        CompletableFuture<?> getFuture() {
            return this.future;
        }

    }

    static class TestCountdownLatchEvent extends TestEvent {

        private final CountDownLatch latch;

        TestCountdownLatchEvent(CountDownLatch latch) {
            this.latch = latch;
        }

        CountDownLatch getLatch() {
            return this.latch;
        }

    }

}