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
package com.github.themrmilchmann.mjl.events;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;

/**
 * An object that takes care of submitting events to subscribers.
 *
 * @since   1.0.0
 *
 * @author  Leon Linhart
 */
public abstract class EventDispatcher {

    /**
     * Returns a dispatcher that dispatches events directly.
     *
     * The dispatcher dispatches all events directly upon receiving them without any additional processing or
     * computation. Thus, events are dispatched in the order in which they are received.
     *
     * @return  a dispatcher that dispatches events directly
     *
     * @since   1.0.0
     */
    public static EventDispatcher directDispatcher() {
        return DirectDispatcher.INSTANCE;
    }

    /**
     * Returns a dispatcher that that guarantees that all events that are posted in a single thread are dispatched to
     * their subscribers in the order they are posted by queuing events that are posted reentrantly on a thread.
     *
     * @return  a dispatcher that guarantees that all events that are posted in a single thread are dispatched to their
     *          subscribers in the order they are posted.
     *
     * @since   1.0.0
     */
    public static EventDispatcher perThreadDispatchQueue() {
        return new PerThreadDispatchQueueDispatcher();
    }

    /**
     * Dispatches the event to the subscribers.
     *
     * <p>Implementations are responsible for
     * {@link com.github.themrmilchmann.mjl.events.EventBus.Subscriber#dispatch(Event) dispatching} events.</p>
     *
     * @param event         the event to dispatch
     * @param subscribers   the subscribers to dispatch the event to
     *
     * @since   1.0.0
     */
    protected abstract void dispatch(Event event, Collection<EventBus.Subscriber> subscribers);

    private static class DirectDispatcher extends EventDispatcher {

        private static final EventDispatcher INSTANCE = new DirectDispatcher();

        @Override
        protected void dispatch(Event event, Collection<EventBus.Subscriber> subscribers) {
            Objects.requireNonNull(event);
            Objects.requireNonNull(subscribers);
            subscribers.forEach(subscriber -> subscriber.dispatch(event));
        }

    }

    private static class PerThreadDispatchQueueDispatcher extends EventDispatcher {

        private final ThreadLocal<Queue<QueuedEvent>> threadLocalQueue = ThreadLocal.withInitial(ArrayDeque::new);
        private final ThreadLocal<Boolean> isThreadDispatching = ThreadLocal.withInitial(() -> false);

        @Override
        protected void dispatch(Event event, Collection<EventBus.Subscriber> subscribers) {
            Objects.requireNonNull(event);
            Objects.requireNonNull(subscribers);

            Queue<QueuedEvent> eventQueue = this.threadLocalQueue.get();
            eventQueue.offer(new QueuedEvent(event, subscribers));

            if (!this.isThreadDispatching.get()) {
                this.isThreadDispatching.set(true);

                try {
                    QueuedEvent queuedEvent;

                    while ((queuedEvent = eventQueue.poll()) != null) {
                        Event e = queuedEvent.event;
                        queuedEvent.subscribers.forEach(subscriber -> subscriber.dispatch(e));
                    }
                } finally {
                    this.threadLocalQueue.remove();
                    this.isThreadDispatching.remove();
                }
            }
        }

        private static class QueuedEvent {

            private final Event event;
            private final Collection<EventBus.Subscriber> subscribers;

            private QueuedEvent(Event event, Collection<EventBus.Subscriber> subscribers) {
                this.event = event;
                this.subscribers = subscribers;
            }

        }

    }

}