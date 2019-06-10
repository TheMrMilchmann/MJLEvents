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

import com.github.themrmilchmann.mjl.events.EventBus;
import org.testng.annotations.Test;

@SuppressWarnings("ConstantConditions")
@Test
public final class BuilderTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$setDispatcher$Nullability() {
        EventBus.builder().setDispatcher(null);
    }

    public void testParams$setDispatchErrorHandler() {
        EventBus.builder().setDispatchErrorHandler(null);
    }

    public void testParams$setDeadEventHandler() {
        EventBus.builder().setDeadEventHandler(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$setExecutor$Nullability() {
        EventBus.builder().setExecutor(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testParams$setSubscriberMarker$Nullability() {
        EventBus.builder().setSubscriberMarker(null);
    }

    private @interface IllegalSubscriberMarker {}

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParams$setSubscriberMarker$Checks() {
        EventBus.builder().setSubscriberMarker(IllegalSubscriberMarker.class);
    }

}