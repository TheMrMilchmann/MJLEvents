/*
 * Copyright 2018-2020 Leon Linhart
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

/**
 * A handler for events that can be manually {@link EventBus#subscribe(Class, EventHandler)} to an {@link EventBus}.
 *
 * @param <E>   the type of the handled events
 *
 * @since   3.0.0
 *
 * @author  Leon Linhart
 */
@FunctionalInterface
public interface EventHandler<E> {

    /**
     * Handles the given event.
     *
     * @param event the event to handle
     *
     * @throws Throwable    any throwable thrown while handling the event
     *
     * @since   3.0.0
     */
    void handle(E event) throws Throwable;

}