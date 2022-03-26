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

import java.lang.invoke.MethodHandles;
import com.github.themrmilchmann.mjl.events.EventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
public final class EventBusTests {

    @Test
    public void testParams$subscribeCls$Nullability() {
        EventBus<?> bus = EventBus.builder().build();

        //noinspection RedundantCast
        assertThrows(NullPointerException.class, () -> bus.subscribe((Class<?>) null, MethodHandles.lookup()));
        assertThrows(NullPointerException.class, () -> bus.subscribe(EventBusTests.class, null));
    }

    @Test
    public void testParams$subscribeObj$Nullability() {
        EventBus<?> bus = EventBus.builder().build();

        assertThrows(NullPointerException.class, () -> bus.subscribe((Object) null, MethodHandles.lookup()));
        assertThrows(NullPointerException.class, () -> bus.subscribe(this, null));
    }

    @Test
    public void testParams$post$Nullability() {
        EventBus<?> bus = EventBus.builder().build();
        assertThrows(NullPointerException.class, () -> bus.post(null));
    }

}