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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * A <i>dispatcher</i> is responsible of submitting events to subscribers.
 *
 * @since   1.0.0
 *
 * @author  Leon Linhart
 */
public abstract class EventDispatcher<E> {

    /**
     * Returns a dispatcher that dispatches events directly.
     *
     * <p>The dispatcher dispatches all events directly upon receiving them without any additional processing or
     * computation. Thus, events are dispatched in the order in which they are received.</p>
     *
     * @param <E>   the type of the events to dispatch
     *
     * @return  a dispatcher that dispatches events directly
     *
     * @since   1.0.0
     */
    @SuppressWarnings("unchecked")
    public static <E> EventDispatcher<E> directDispatcher() {
        return (DirectDispatcher<E>) DirectDispatcher.INSTANCE;
    }

    /**
     * Returns a dispatcher that that guarantees that all events that are posted in a single thread are dispatched to
     * their subscribers in the order they are posted by queuing events that are posted reentrantly on a thread.
     *
     * @param <E>   the type of the events to dispatch
     *
     * @return  a dispatcher that guarantees that all events that are posted in a single thread are dispatched to their
     *          subscribers in the order they are posted.
     *
     * @since   1.0.0
     */
    public static <E> EventDispatcher<E> perThreadDispatchQueue() {
        return new PerThreadDispatchQueueDispatcher<>();
    }

    /**
     * Dispatches the event to the subscribers.
     *
     * <p>Implementations are responsible for
     * {@link com.github.themrmilchmann.mjl.events.EventBus.Subscriber#dispatch(Object) dispatching} events.</p>
     *
     * @param <T>           the type of the event to dispatch
     * @param event         the event to dispatch
     * @param subscribers   the subscribers to dispatch the event to
     *
     * @since   1.0.0
     */
    protected abstract <T extends E> void dispatch(T event, Set<EventBus.Subscriber<? super T>> subscribers);

    private static class DirectDispatcher<E> extends EventDispatcher<E> {

        private static final EventDispatcher<?> INSTANCE = new DirectDispatcher<>();

        @Override
        protected <T extends E> void dispatch(T event, Set<EventBus.Subscriber<? super T>> subscribers) {
            Objects.requireNonNull(event);
            Objects.requireNonNull(subscribers);
            subscribers.forEach(subscriber -> subscriber.dispatch(event));
        }

    }

    private static class PerThreadDispatchQueueDispatcher<E> extends EventDispatcher<E> {

        private final ThreadLocal<Queue<QueuedEvent<? extends E>>> threadLocalQueue = ThreadLocal.withInitial(ArrayDeque::new);
        private final ThreadLocal<Boolean> isThreadDispatching = ThreadLocal.withInitial(() -> false);

        @Override
        protected <T extends E> void dispatch(T event, Set<EventBus.Subscriber<? super T>> subscribers) {
            Objects.requireNonNull(event);
            Objects.requireNonNull(subscribers);

            Queue<QueuedEvent<? extends E>> eventQueue = this.threadLocalQueue.get();
            eventQueue.offer(new QueuedEvent<>(event, subscribers));

            if (!this.isThreadDispatching.get()) {
                this.isThreadDispatching.set(true);

                try {
                    QueuedEvent<? extends E> queuedEvent;

                    while ((queuedEvent = eventQueue.poll()) != null) {
                        queuedEvent.dispatch();
                    }
                } finally {
                    this.threadLocalQueue.remove();
                    this.isThreadDispatching.remove();
                }
            }
        }

        private static class QueuedEvent<E> {

            private final E event;
            private final Collection<EventBus.Subscriber<? super E>> subscribers;

            private QueuedEvent(E event, Collection<EventBus.Subscriber<? super E>> subscribers) {
                this.event = event;
                this.subscribers = subscribers;
            }

            private void dispatch() {
                this.subscribers.forEach(subscriber -> subscriber.dispatch(this.event));
            }

        }

    }

}