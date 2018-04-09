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
 * A {@code DispatchErrorHandler} may be used to handle errors that occur while dispatching an event to a subscriber.
 * 
 * @see EventBus.Builder#setDispatchErrorHandler(DispatchErrorHandler)
 *
 * @since   1.1.0
 *
 * @author  Leon Linhart
 */
public interface DispatchErrorHandler {

    /**
     * Handle an error that occurred while dispatching an event to a subscriber.
     *
     * @param event         the event that could not be dispatched
     * @param subscriber    the subscriber that the event could not be dispatched to
     * @param error         the error that occurred while dispatching the event to the subscriber
     *
     * @since   1.1.0
     */
    void onDispatchError(Event event, EventBus.Subscriber subscriber, Throwable error);

}