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

import com.github.themrmilchmann.mjl.events.EventBus;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;

@Test
public final class EventBusTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$subscribeCls$Nullability() {
        EventBus bus = new EventBus.Builder().build();
        bus.register(null, MethodHandles.lookup());
        bus.register(EventBusTests.class, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$subscribeObj$Nullability() {
        EventBus bus = new EventBus.Builder().build();
        bus.register(null, MethodHandles.lookup());
        bus.register(this, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$unsubscribe$Nullability() {
        EventBus bus = new EventBus.Builder().build();
        bus.unregister(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$post$Nullability() {
        EventBus bus = new EventBus.Builder().build();
        bus.post(null);
    }

}