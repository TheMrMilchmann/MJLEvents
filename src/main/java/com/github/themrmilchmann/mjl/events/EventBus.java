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

import com.github.themrmilchmann.mjl.events.util.concurrent.MJLExecutors;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * An EventBus for publish-subscribe-style communication between components.
 *
 * <h2>Posting events</h2>
 *
 * <p>To post an event, simply pass the event object to the {@link #post(Event)} method. The bus will dispatch the event
 * to all registered subscribers whose accepted type {@link Class#isAssignableFrom(Class) is assignable from} the
 * event's type.</p>
 *
 *
 * <h2>Subscribers</h2>
 *
 * <p>To receive events, a method, known as the <i>subscriber</i>, which accepts a single argument of the type of the
 * desired event must be marked with the bus' subscriber marker annotation and its enclosing class must be registered to
 * the bus.</p>
 *
 *
 * <h2>Dead Events</h2>
 *
 * <p>If a posted event cannot be received by any subscriber, it is considered "dead". Dead events are wrapped in an
 * instance of {@link DeadEvent} and posted again. Subscribing to these events can be useful for debugging purposes.</p>
 **
 * <p>A {@code DeadEvent} is dispatched only to subscribers who explicitly expect it. (A subscriber method which expects
 * the common supertype {@code Event} will not receive dead events.</p>
 *
 *
 * <h2>Thread-safety</h2>
 *
 * <p>All methods in this class are thread-safe by default. (Depending on the {@link EventDispatcher dispatcher} and
 * {@link Executor executor} implementations in use, the {@linkplain #post(Event) post} method might be unsafe. In this
 * case, it should be specified in the respective implementation's documentation explicitly.)</p>
 *
 *
 * <h2>Memory management</h2>
 *
 * <p>By default, it is not guaranteed that a EventBus removes all references to unused objects when
 * {@link #unregister(Object)} is called. Those references may be manually removed by calling {@link #clear()}.</p>
 *
 * <p>Alternatively it is possible to overwrite this behavior by creating a <i>self-cleaning</i> bus (via
 * {@link Builder#setSelfCleaning(boolean)}). A bus created with this option will always remove all unused references as
 * soon as possible at the cost of computation speed for registering/unregistering operations.</p>
 *
 *
 * @since   1.0.0
 *
 * @author  Leon Linhart
 */
public final class EventBus {

    private final ConcurrentMap<Class<? extends Event>, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    private final EventDispatcher dispatcher;
    private final Executor executor;
    private final Class<? extends Annotation> subscriberMarker;
    private final boolean isSelfCleaning;

    private EventBus(EventDispatcher dispatcher, Executor executor, Class<? extends Annotation> subscriberMarker, boolean isSelfCleaning) {
        this.dispatcher = dispatcher;
        this.executor = executor;
        this.subscriberMarker = subscriberMarker;
        this.isSelfCleaning = isSelfCleaning;
    }

    /**
     * The annotation type that is used to identify methods that are potential subscribers to events on this bus.
     *
     * @return  the annotation type that is used to identify methods that are potential subscribers to events on this
     *          bus
     *
     * @see #register(Class, MethodHandles.Lookup)
     * @see #register(Object, MethodHandles.Lookup)
     * @see Builder#setSubscriberMarker(Class)
     *
     * @since   1.0.0
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public Class<? extends Annotation> getSubscriberMarker() {
        return this.subscriberMarker;
    }

    /**
     * Registers all static methods of the given class that are visible to the given lookup and
     * {@link #getSubscriberMarker() marked} with this bus' marker.
     *
     * <p>Searches the given object's class for any non-synthetic instance method that is marked with this bus'
     * subscriber marker and performs several vanity checks on each of them and throws an
     * {@link IllegalArgumentException} if any of them:</p>
     * <ul>
     *     <li>expects any number of parameters other than one, or {@link Event} is not assignable from
     *     that parameter's type,</li>
     *     <li>returns any other type than {@code void}, or</li>
     *     <li>if the method is not visible to the given lookup.</li>
     * </ul>
     *
     * <p>If all checks succeed, a {@link Subscriber} object for each subscriber method is registered to this bus.</p>
     *
     * @param cls       the class, which's statically accessible methods should be subscribed to this bus (the passed
     *                  object is considered to be the subscribers origin)
     * @param lookup    the lookup that is used to create subscriber objects
     *
     * @throws NullPointerException     if the given class or the given lookup is {@code null}
     * @throws IllegalArgumentException if a method of the given class is statically accessible and marked as subscriber
     *                                  but is not visible to the given lookup or expects unsupported parameters
     *
     * @see #register(Object, MethodHandles.Lookup)
     * @see #unregister(Object)
     *
     * @since   1.0.0
     */
    @SuppressWarnings({"unused", "unchecked", "WeakerAccess"})
    public void register(Class<?> cls, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(lookup);

        Map<Class<? extends Event>, Collection<Subscriber>> eventSubscribers = new HashMap<>();
        Method[] methods = cls.getDeclaredMethods();

        for (Method method : methods) {
            int mods = method.getModifiers();

            if (!method.isSynthetic() && method.isAnnotationPresent(subscriberMarker) && Modifier.isStatic(mods)) {
                if (method.getReturnType() != void.class)
                    throw new IllegalArgumentException("Subscriber method must have void return type");

                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
                    throw new IllegalArgumentException("Subscriber method must only have one parameter of a type that Event can be assigned to");

                try {
                    MethodHandle methodHandle = lookup.unreflect(method);
                    Class<? extends Event> eventType = (Class<? extends Event>) methodHandle.type().parameterList().get(0);
                    Subscriber subscriber = new Subscriber(this, cls, method, methodHandle);

                    eventSubscribers.computeIfAbsent(eventType, k -> new ArrayList()).add(subscriber);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Unexpected exception during subscriber creation", e);
                }
            }
        }

        eventSubscribers.forEach((eventType, subscribers) -> this.subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList()).addAll(subscribers));
    }

    /**
     * Registers all instance methods of the given object's class that are visible to the given lookup and
     * {@link #getSubscriberMarker() marked} with this bus' marker.
     *
     * <p>Searches the given object's class for any non-synthetic instance method that is marked with this bus'
     * subscriber marker and performs several vanity checks on each of them and throws an
     * {@link IllegalArgumentException} if any of them:</p>
     * <ul>
     *     <li>expects any number of parameters other than one, or {@link Event} is not assignable from
     *     that parameter's type,</li>
     *     <li>returns any other type than {@code void}, or</li>
     *     <li>if the method is not visible to the given lookup.</li>
     * </ul>
     *
     * <p>If all checks succeed, a {@link Subscriber} object for each subscriber method is registered to this bus.</p>
     *
     * @param object    the object, which's instance accessible methods should be subscribed to this bus (the passed
     *                  object is considered to be the subscribers origin)
     * @param lookup    the lookup that is used to create subscriber objects
     *
     * @throws NullPointerException     if the given object or the given lookup is {@code null}
     * @throws IllegalArgumentException if a method of the given class is instance accessible and marked as subscriber
     *                                  but is not visible to the given lookup or expects unsupported parameters
     *
     * @see #register(Class, MethodHandles.Lookup)
     * @see #unregister(Object)
     *
     * @since   1.0.0
     */
    @SuppressWarnings({"unused", "unchecked", "WeakerAccess"})
    public void register(Object object, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(object);
        Objects.requireNonNull(lookup);

        Map<Class<? extends Event>, Collection<Subscriber>> eventSubscribers = new HashMap<>();
        Method[] methods = object.getClass().getDeclaredMethods();

        for (Method method : methods) {
            int mods = method.getModifiers();

            if (!method.isSynthetic() && method.isAnnotationPresent(subscriberMarker) && !Modifier.isStatic(mods)) {
                if (method.getReturnType() != void.class)
                    throw new IllegalArgumentException("Subscriber method must have void return type");

                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
                    throw new IllegalArgumentException("Subscriber method must only have one parameter of a type that Event can be assigned to");

                try {
                    MethodHandle methodHandle = lookup.unreflect(method).bindTo(object);
                    Class<? extends Event> eventType = (Class<? extends Event>) method.getParameterTypes()[0];
                    Subscriber subscriber = new Subscriber(this, object, method, methodHandle);

                    eventSubscribers.computeIfAbsent(eventType, k -> new ArrayList()).add(subscriber);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Unexpected exception during subscriber creation", e);
                }
            }
        }

        eventSubscribers.forEach((eventType, subscribers) -> this.subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList()).addAll(subscribers));
    }

    /**
     * Unregisters each subscriber, whose origin is equal to the given object, from this bus.
     *
     * <p>If this bus is not self-cleaning, this method might not remove all references to unneeded objects which may
     * result into memory leaks if not handled appropriately. These references may be removed manually by calling
     * {@link #clear()}.</p>
     *
     * @param object    the origin of the subscribers that should be unregistered from this bus
     *
     * @throws NullPointerException if the given object is {@code null}
     *
     * @since   1.0.0
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void unregister(Object object) {
        Objects.requireNonNull(object);

        if (this.isSelfCleaning) {
            synchronized (this) {
                this.subscribers.entrySet().removeIf(entry -> entry.getValue().removeIf(subscriber -> subscriber.origin.equals(object)));
            }
        } else {
            this.subscribers.values().forEach(subscribers -> subscribers.removeIf(subscriber -> subscriber.origin.equals(object)));
        }
    }

    /**
     * Removes all references to empty or unused internal objects that are too expensive to remove otherwise.
     *
     * <p>If this bus is self-cleaning, this method does nothing instead.</p>
     *
     * @see EventBus.Builder#setSelfCleaning(boolean)
     *
     * @since   1.0.0
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void clear() {
        if (!this.isSelfCleaning) {
            synchronized (this) {
                this.subscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
        }
    }

    /**
     * Unregisters all subscribers of this bus and {@linkplain #clear() clears} the internal state, effectively
     * resetting the bus to its initial state.
     *
     * @since   1.0.0
     */
    @SuppressWarnings("unused")
    public void purge() {
        synchronized (this) {
            subscribers.clear();
        }
    }

    /**
     * Posts the given {@linkplain Event} to this bus.
     *
     * <p>The event will be {@link EventDispatcher#dispatch(Event, Collection) dispatched} to all subscribers on this
     * bus whose accepted type {@link Class#isAssignableFrom(Class) is assignable from} the event's type.</p>
     *
     * <p>If a posted event cannot be received by any subscriber, it is considered "dead". Dead events are wrapped in an
     * instance of {@link DeadEvent} and posted again. Subscribing to these events can be useful for debugging purposes.
     * (Unlike any other event, a dead event will only be dispatched to subscribers that explicitly accept a
     * {@linkplain DeadEvent}.</p>
     *
     * <p>The subscribers are called in no particular order, some time in the future and no guarantees are made that the
     * events are passed to the subscribers in the order in which they are posted to the bus. This behaviour may be
     * changed at the discretion of this bus' {@link EventDispatcher} and {@link Executor} implementation respectively.
     * However, it is guaranteed, that an event will only be dispatched to the subscribers that were subscribed to the
     * event before the event was posted.</p>
     *
     * @param event the event to be dispatched to this bus' subscribers
     *
     * @throws NullPointerException if the given event is {@code null}
     *
     * @since   1.0.0
     */
    public void post(Event event) {
        Objects.requireNonNull(event);

        List<Subscriber> eventSubscribers = this.subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().isAssignableFrom(event.getClass()))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());

        if (!eventSubscribers.isEmpty()) {
            dispatcher.dispatch(event, eventSubscribers);
        } else if (!(event instanceof DeadEvent)) {
            eventSubscribers.removeIf(entry -> entry.method.getParameterTypes()[0] != DeadEvent.class);
            post(new DeadEvent(event));
        }
    }

    /**
     * A wrapper class for a subscriber method.
     *
     * <p>This class provides several methods that serve as proxies to the underlying subscriber method and may be used
     * by a {@link EventDispatcher dispatcher} to make decisions about the execution order.</p>
     *
     * @since   1.0.0
     */
    public static final class Subscriber {

        private final EventBus bus;
        private final Object origin;
        private final Method method;
        private final MethodHandle handle;

        private Subscriber(EventBus bus, Object origin, Method method, MethodHandle handle) {
            this.bus = bus;
            this.origin = origin;
            this.method = method;
            this.handle = handle;
        }

        /**
         * Dispatches the given {@link Event} to the wrapped subscriber.
         *
         * <p>The blocking behaviour of this method depends on the executor implementation of the {@link EventBus} that
         * the subscriber is registered to.</p>
         *
         * @param event the event to dispatch
         *
         * @throws NullPointerException if the given event is {@code null}
         *
         * @since   1.0.0
         */
        @SuppressWarnings("WeakerAccess")
        public void dispatch(Event event) {
            Objects.requireNonNull(event);

            this.bus.executor.execute(() -> {
                try {
                    this.handle.invokeWithArguments(event);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }

        /**
         * <b>Serves as a proxy for {@link Method#getAnnotation(Class)}.</b>
         *
         * @param <T>               the type of the annotation to query for and return if present
         * @param annotationClass   the Class object corresponding to the annotation type
         *
         * @return this element's annotation for the specified annotation type if present on this element, else null
         *
         * @throws NullPointerException if the given annotation class is null
         *
         * @since   1.0.0
         */
        @SuppressWarnings("unused")
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return this.method.getAnnotation(annotationClass);
        }

        /**
         * <b>Serves as a proxy for {@link Method#getAnnotationsByType(Class)}.</b>
         *
         * @param <T>               the type of the annotation to query for and return if present
         * @param annotationClass   the Class object corresponding to the annotation type
         *
         * @return  all this element's annotations for the specified annotation type if associated with this element,
         *          else an array of length zero
         *
         * @throws NullPointerException if the given annotation class is null
         *
         * @since   1.0.0
         */
        @SuppressWarnings("unused")
        public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
            return this.method.getAnnotationsByType(annotationClass);
        }

        /**
         * <b>Serves as a proxy for {@link Method#isAnnotationPresent(Class)}.</b>
         *
         * @param annotationClass   the Class object corresponding to the annotation type
         *
         * @return  true if an annotation for the specified annotation type is present on this element, else false
         *
         * @throws NullPointerException if the given annotation class is null
         *
         * @since   1.0.0
         */
        @SuppressWarnings("unused")
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return this.method.isAnnotationPresent(annotationClass);
        }

    }

    /**
     * A builder class for an {@link EventBus}.
     *
     * <p>A builder is reusable. Calling {@linkplain #build()} does not reset nor does it invalidate the builder.</p>
     *
     * @since   1.0.0
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        private EventDispatcher dispatcher;
        private Executor executor;
        private Class<? extends Annotation> subscriberMarker;
        private boolean isSelfCleaning;

        /**
         * Creates a new builder instance.
         *
         * @since   1.0.0
         */
        public Builder() {
            this.dispatcher = EventDispatcher.perThreadDispatchQueue();
            this.executor = MJLExecutors.directExecutor();
            this.subscriberMarker = EventSubscriber.class;
            this.isSelfCleaning = false;
        }

        /**
         * Creates and returns a new bus with the settings currently defined in this builder.
         *
         * @return  a new bus with the settings as currently specified in this builder
         *
         * @since   1.0.0
         */
        public EventBus build() {
            return new EventBus(this.dispatcher, this.executor, this.subscriberMarker, this.isSelfCleaning);
        }

        /**
         * Sets the dispatcher implementation that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * @param dispatcher    the dispatcher for the bus
         *
         * @return  this builder instance
         *
         * @throws NullPointerException if the given dispatcher is {@code null}
         *
         * @since   1.0.0
         */
        public Builder setDispatcher(EventDispatcher dispatcher) {
            this.dispatcher = Objects.requireNonNull(dispatcher);
            return this;
        }

        /**
         * Sets the executor implementation that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * @param executor  the executor for the bus
         *
         * @return  this builder instance
         *
         * @throws NullPointerException if the given executor is {@code null}
         *
         * @since   1.0.0
         */
        public Builder setExecutor(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
            return this;
        }

        /**
         * Sets whether or not the bus will be self-cleaning.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * @param value whether or not the bus should be self-cleaning
         *
         * @return  this builder instance
         *
         * @see EventBus#clear()
         * @see EventBus#unregister(Object)
         *
         * @since   1.0.0
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setSelfCleaning(boolean value) {
            this.isSelfCleaning = value;
            return this;
        }

        /**
         * Sets the subscriber marker that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * @param type  the marker type
         *
         * @return  this builder instance
         *
         * @throws NullPointerException     if the given type is {@code null}
         * @throws IllegalArgumentException if the given type is not visible at runtime
         *
         * @see EventBus#register(Class, MethodHandles.Lookup)
         * @see EventBus#register(Object, MethodHandles.Lookup)
         *
         * @since   1.0.0
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setSubscriberMarker(Class<? extends Annotation> type) {
            type = Objects.requireNonNull(type);
            Retention retention = type.getAnnotation(Retention.class);

            if (retention == null || retention.value() != RetentionPolicy.RUNTIME)
                throw new IllegalArgumentException(type + " is not visible at runtime");

            this.subscriberMarker = Objects.requireNonNull(type);
            return this;
        }

    }

}