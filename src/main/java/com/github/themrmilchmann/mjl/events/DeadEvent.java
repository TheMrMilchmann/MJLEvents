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
package com.github.themrmilchmann.mjl.events;

/**
 * A specialized {@link Event} that is {@link EventBus#post(Event) posted} whenever an event without any listening
 * subscribers is posted.
 *
 * <p>This event is mainly useful for debugging purposed.</p>
 *
 * @see EventBus#post(Event)
 *
 * @since   1.0.0
 *
 * @author  Leon Linhart
 */
public final class DeadEvent implements Event {

    private final Event event;

    DeadEvent(Event event) {
        this.event = event;
    }

    /**
     * The event that was originally posted to the bus but that no subscribers listened to.
     *
     * @return  the event that was originally posted to the bus but that no subscribers listened to
     *
     * @since   1.0.0
     */
    @SuppressWarnings("unused")
    public Event getEvent() {
        return this.event;
    }

}