/*
 * Copyright 2018-2021 Leon Linhart
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
package com.github.themrmilchmann.mjl.events;

import java.lang.invoke.MethodHandles;
import javax.annotation.Nullable;

/**
 * A handle to one or more event handlers subscribed to an {@link EventBus}.
 *
 * <p>A handle might either be created for a single {@link EventHandler} instance, if it is {@link EventBus#subscribe(Class, EventHandler)
 * subscribed} explicitly, or for multiple methods when using {@link EventBus#subscribe(Class, MethodHandles.Lookup)
 * annotation-based discovery}.</p>
 *
 * @since   3.0.0
 *
 * @author  Leon Linhart
 */
public final class SubscriberHandle {

    private final Runnable unsubscribe;

    @Nullable
    private Object ref;

    SubscriberHandle(Runnable unsubscribe, Object ref) {
        this.unsubscribe = unsubscribe;
        this.ref = ref;
    }

    /**
     * Unsubscribes the handlers represented by this handle from their bus. If they have already been unsubscribed, this
     * method does nothing.
     *
     * @since   3.0.0
     */
    public void unsubscribe() {
        if (this.ref == null) return;

        this.unsubscribe.run();
        this.ref = null;
    }

}