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
package com.github.themrmilchmann.mjl.events;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import javax.annotation.Nullable;
import com.github.themrmilchmann.mjl.events.internal.ConcurrentWeakHashMap;

/**
 * An {@code EventBus} for publish-subscribe-style communication between components.
 *
 * <p>To receive events, a handler, referred to as subscriber, which accepts a single parameter of the type of the
 * desired events must be {@link #subscribe(Class, EventHandler) subscribed} to the bus. Instead of explicitly
 * subscribing a handler to an event, it is also possible to automatically create handlers for an object or class using
 * {@link #subscribe(Class, MethodHandles.Lookup) annotation-based subscriber discovery}. The handler will then be
 * applicable to receive any event whose type {@link Class#isAssignableFrom(Class) is assignable to} its parameter type.
 * </p>
 *
 * <p>Objects may be {@link #post(Object) posted} to the bus. (Once an object has been posted, it is loosely referred to
 * as <em>event</em>.) The bus' {@link EventDispatcher dispatcher} then takes care of delivering the object to all
 * applicable subscribers. If there are no applicable subscribers for an event, it is considered to be <em>dead</em>.
 * Dead events are sometimes an indication of a misconfiguration and can be caught by setting up a
 * {@link EventBus.Builder#setDeadEventHandler(Consumer) special handler}.</p>
 *
 * <p>All methods in this class are thread-safe by default. However, dispatcher and {@link Executor executor}
 * implementations are not generally required to be thread-safe and thus, depending on the configuration, {@link #post(Object)
 * posting} events might not be either. (Dispatchers and executors that are not thread-safe should contain appropriate
 * notes in their documentation.)</p>
 *
 * <p>By default, a bus will automatically remove all references to unused objects when subscribers are
 * {@link SubscriberHandle#unsubscribe() unsubscribed}. In environments where handlers frequently subscribe to and
 * unsubscribe from a bus, this might lead to poor performance. Alternatively, {@link EventBus.Builder#setSelfCleaning(boolean)
 * it is possible to disable automatic cleanup} and to manually {@link #cleanup()} the bus.</p>
 *
 * @since   1.0.0
 *
 * @author  Leon Linhart
 */
public final class EventBus<E> {

    /**
     * Returns a builder for an {@code EventBus}.
     *
     * @return  a builder instance
     *
     * @since   3.0.0
     */
    public static Builder<Object> builder() {
        return new Builder<>(Object.class);
    }

    /**
     * Returns a builder for an {@code EventBus}.
     *
     * @param <E>   the type of the events
     * @param cls   the type of the events
     *
     * @return  a builder instance
     *
     * @since   3.0.0
     */
    public static <E> Builder<E> builder(Class<E> cls) {
        return new Builder<>(cls);
    }

    /*
     * A couple of notes about GC eligibility of the related objects here:
     *
     * As long as a SubscriberHandle is strongly referenced somewhere (and SubscriberHandle#unsubscribe has not been
     * called), the EventHandler (or Context for MethodHandles) is still strongly referenced. These, in turn, implicitly
     * reference the "eventType" class (the one which serves as key to the subscribers map) via their signature.
     * Therefore, keys and (thus) values of the "subscribers" map are not eligible for GC.
     *
     * Subscribers do not hold a strong reference to the actual handlers. Thus, if the SubscriberHandle becomes eligible
     * for GC, EventHandlers are unsubscribed (unless there is another strong reference in consumer-code), but
     * MethodHandle-based handlers are still valid (since their "eventType" classes are still referenced).
     */
    private final ConcurrentWeakHashMap<Class<? extends E>, CopyOnWriteArraySet<Subscriber<? extends E>>> subscribers = new ConcurrentWeakHashMap<>();
    private final ConcurrentWeakHashMap<Class<? extends E>, Set<Class<? extends E>>> receivingTypes = new ConcurrentWeakHashMap<>();

    private final Class<E> type;
    private final EventDispatcher<E> dispatcher;

    @Nullable
    private final DispatchErrorHandler<E> dispatchErrorHandler;
    private final Executor executor;
    private final Class<? extends Annotation> subscriberMarker;
    private final boolean isSelfCleaning;
    private final Function<Class<?>, Method[]> mapper;

    @Nullable
    private final Consumer<E> deadEventHandler;

    private EventBus(Builder<E> builder) {
        this.type = builder.type;
        this.dispatcher = builder.dispatcher;
        this.dispatchErrorHandler = builder.dispatchErrorHandler;
        this.executor = builder.executor;
        this.subscriberMarker = builder.subscriberMarker;
        this.isSelfCleaning = builder.isSelfCleaning;
        this.mapper = builder.mapper;
        this.deadEventHandler = builder.deadEventHandler;
    }

    /**
     * Returns the base type for events for this bus.
     *
     * @return  the base types for events for this bus
     *
     * @since   3.0.0
     */
    public Class<?> getEventType() {
        return this.type;
    }

    /**
     * The annotation that is used to identify methods that are potential subscribers to events on this bus during
     * {@link #subscribe(Class, MethodHandles.Lookup) annotation-based subscriber discovery}.
     *
     * @return  the annotation type that is used to identify methods that are potential subscribers to events on this
     *          bus
     *
     * @see #subscribe(Class, MethodHandles.Lookup)
     * @see #subscribe(Object, MethodHandles.Lookup)
     * @see Builder#setSubscriberMarker(Class)
     *
     * @since   1.0.0
     */
    public Class<? extends Annotation> getSubscriberMarker() {
        return this.subscriberMarker;
    }

    /**
     * Subscribes the given handler to this bus for the given type of events.
     *
     * <p>The given {@code handler} is only weakly referenced from this bus. To prevent premature garbage collection, a
     * strong reference must be held (for example by holding onto the returned handle).</p>
     *
     * @param <T>       the type of the event
     * @param eventType the type of the event
     * @param handler   the handler function for the event
     *
     * @return  a handle for the methods that have been subscribed to this, or {@code null}
     *
     * @throws NullPointerException     if any parameter is {@code null}
     *
     * @since   3.0.0
     */
    public <T extends E> SubscriberHandle subscribe(Class<T> eventType, EventHandler<T> handler) {
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(handler);

        Subscriber<T> subscriber = new Subscriber.SubscribedEventHandler<>(this, handler);
        CopyOnWriteArraySet<Subscriber<? extends E>> subscribers = this.subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>());
        subscribers.add(subscriber);

        WeakReference<CopyOnWriteArraySet<Subscriber<? extends E>>> subscribersRef = new WeakReference<>(subscribers);
        WeakReference<Subscriber<T>> subscriberRef = new WeakReference<>(subscriber);

        Runnable unsubscribe = () -> {
            CopyOnWriteArraySet<Subscriber<? extends E>> subs = subscribersRef.get();
            if (subs == null) return;

            Subscriber<T> sub = subscriberRef.get();
            if (sub == null) return;

            subs.remove(sub);
            this.unsubscribe();
        };

        return new SubscriberHandle(unsubscribe, handler);
    }

    /**
     * Dynamically discovers potential event-handling {@code static} methods in the given class using this bus'
     * {@link #getSubscriberMarker() subscriber marker}, attempts to subscribe all of them to this bus and returns a
     * {@link SubscriberHandle handle} for the subscribed methods, or {@code null} if no such methods were found.
     *
     * <p>This bus' {@link EventBus.Builder#setMethodMapper(Function) method mapper} is used to discover all methods
     * from the given class. All {@code static} non-{@link Method#isBridge() bridge} methods that are annotated with
     * this bus' {@link #getSubscriberMarker() marker} are considered to be candidates. If no such candidate exists,
     * {@code null} is returned. Otherwise, multiple checks are performed for each candidate method and an exception is
     * thrown if any of them:</p>
     * <ul>
     * <li>expects any number of parameters other than one, or this bus' {@link #getEventType() base type} is not
     * {@link Class#isAssignableFrom(Class) assignable from} that parameter's type,</li>
     * <li>returns any other type than {@code void}, or</li>
     * <li>if the method is not visible to the given lookup.</li>
     * </ul>
     *
     * <p>If all checks succeed for every candidate, all of them are subscribed to this bus and a handle for them is
     * returned. The given class is considered to be the {@link Subscriber#getOrigin() origin} for the new subscribers.
     * </p>
     *
     * <p>The given {@code Class} object is only weakly referenced from this bus. To prevent premature garbage
     * collection, a strong reference must be held (for example by holding onto the returned handle).</p>
     *
     * @param cls       the class whose methods should be searched for subscribers
     * @param lookup    the lookup to be used to access the classes methods
     *
     * @return  a handle for the methods that have been subscribed to this, or {@code null}
     *
     * @throws IllegalArgumentException if a {@code MethodHandle} could not be created for a subscriber
     * @throws NullPointerException     if any parameter is {@code null}
     *
     * @see #subscribe(Class, EventHandler)
     * @see #subscribe(Object, MethodHandles.Lookup)
     *
     * @since   3.0.0
     */
    @Nullable
    public SubscriberHandle subscribe(Class<?> cls, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(lookup);

        Method[] methods = this.mapper.apply(cls);

        //noinspection NullableProblems
        return this.subscribe(methods, cls, Modifier::isStatic, lookup::unreflect);
    }

    /**
     * Dynamically discovers potential event-handling non-{@code static} methods in the given class using this bus'
     * {@link #getSubscriberMarker() subscriber marker}, attempts to subscribe all of them to this bus and returns a
     * {@link SubscriberHandle handle} for the subscribed methods, or {@code null} if no such methods were found.
     *
     * <p>This bus' {@link EventBus.Builder#setMethodMapper(Function) method mapper} is used to discover all methods
     * from the given class. All non-{@code static} non-{@link Method#isBridge() bridge} methods that are annotated with
     * this bus' {@link #getSubscriberMarker() marker} are considered to be candidates. If no such candidate exists,
     * {@code null} is returned. Otherwise, multiple checks are performed for each candidate method and an exception is
     * thrown if any of them:</p>
     * <ul>
     * <li>expects any number of parameters other than one, or this bus' {@link #getEventType() base type} is not
     * {@link Class#isAssignableFrom(Class) assignable from} that parameter's type,</li>
     * <li>returns any other type than {@code void}, or</li>
     * <li>if the method is not visible to the given lookup.</li>
     * </ul>
     *
     * <p>If all checks succeed for every candidate, all of them are subscribed to this bus and a handle for them is
     * returned. The given object is considered to be the {@link Subscriber#getOrigin() origin} for the new subscribers.
     * </p>
     *
     * <p>The given {@code instance} is only weakly referenced from this bus. To prevent premature garbage collection, a
     * strong reference must be held (for example by holding onto the returned handle).</p>
     *
     * @param instance  the object whose methods should be searched for subscribers
     * @param lookup    the lookup to be used to access the classes methods
     *
     * @return  a handle for the methods that have been subscribed to this, or {@code null}
     *
     * @throws IllegalArgumentException if a {@code MethodHandle} could not be created for a subscriber
     * @throws NullPointerException     if any parameter is {@code null}
     *
     * @see #subscribe(Class, EventHandler)
     * @see #subscribe(Class, MethodHandles.Lookup)
     *
     * @since   3.0.0
     */
    @Nullable
    public SubscriberHandle subscribe(Object instance, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(lookup);

        Class<?> cls = instance.getClass();
        Method[] methods = this.mapper.apply(cls);

        return this.subscribe(methods, instance, mod -> !Modifier.isStatic(mod), method -> lookup.unreflect(method).bindTo(instance));
    }

    @Nullable
    private SubscriberHandle subscribe(
        Method[] methods,
        Object instance,
        IntPredicate modifier,
        MethodHandleFactory factory
    ) {
        Map<Class<? extends E>, Collection<Subscriber<E>>> eventSubscribers = new HashMap<>();
        List<MethodHandle> methodHandles = new ArrayList<>();

        for (Method method : methods) {
            if (!method.isAnnotationPresent(this.subscriberMarker) || method.isBridge() || !modifier.test(method.getModifiers())) continue;

            if (method.getReturnType() != void.class)
                throw new IllegalArgumentException("Subscriber method must have void return type");

            if (method.getParameterCount() != 1 || !this.type.isAssignableFrom(method.getParameterTypes()[0]))
                throw new IllegalArgumentException("Subscriber method must only have one parameter of a type that Event can be assigned to");

            MethodHandle hMethod;

            try {
                hMethod = factory.apply(method);
            } catch (IllegalAccessException | SecurityException e) {
                throw new IllegalArgumentException("Failed to create MethodHandle", e);
            }

            @SuppressWarnings("unchecked") Class<? extends E> eventType = (Class<? extends E>) method.getParameterTypes()[0];
            Subscriber<E> subscriber = new Subscriber.SubscribedMethodHandle<>(this, instance, method, hMethod);
            eventSubscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
            methodHandles.add(hMethod);
        }

        WeakHashMap<CopyOnWriteArraySet<Subscriber<? extends E>>, WeakHashMap<Subscriber<? extends E>, Void>> typeSubscribersRefs = new WeakHashMap<>();
        eventSubscribers.forEach((eventType, subscribers) -> {
            CopyOnWriteArraySet<Subscriber<? extends E>> typeSubscribers = this.subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>());
            WeakHashMap<Subscriber<? extends E>, Void> subscribersRefs = new WeakHashMap<>();

            for (Subscriber<? extends E> subscriber : subscribers) {
                subscribersRefs.put(subscriber, null);
            }

            this.subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).addAll(subscribers);
            typeSubscribersRefs.put(typeSubscribers, subscribersRefs);
        });

        Runnable unsubscribe = () -> {
            for (Map.Entry<CopyOnWriteArraySet<Subscriber<? extends E>>, WeakHashMap<Subscriber<? extends E>, Void>> entry : typeSubscribersRefs.entrySet()) {
                CopyOnWriteArraySet<Subscriber<? extends E>> typeSubscribers = entry.getKey();
                typeSubscribers.removeIf(it -> entry.getValue().containsKey(it));
            }

            this.unsubscribe();
        };

        return !methodHandles.isEmpty() ? new SubscriberHandle(unsubscribe, methodHandles.toArray(new MethodHandle[0])) : null;
    }

    @FunctionalInterface
    private interface MethodHandleFactory {
        MethodHandle apply(Method method) throws IllegalAccessException;
    }

    private void unsubscribe() {
        if (this.isSelfCleaning) {
            synchronized (this) {
                this.subscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
        }
    }

    /**
     * Removes all references to unused objects if this bus is not {@link EventBus.Builder#setSelfCleaning(boolean)
     * configured to do automatic cleanup}, otherwise this method does nothing.
     *
     * @since   1.0.0
     */
    public void cleanup() {
        if (!this.isSelfCleaning) {
            synchronized (this) {
                this.subscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
        }
    }

    /**
     * Unsubscribes all subscribers from this bus and resets the internal state.
     *
     * @since   1.0.0
     */
    public void reset() {
        synchronized (this) {
            this.subscribers.clear();
            this.receivingTypes.clear();
        }
    }

    /**
     * Posts the given {@code event} to this bus.
     *
     * <p>The event will be {@link EventDispatcher#dispatch(Object, Set) dispatched} to all subscribers on this bus
     * whose accepted type {@link Class#isAssignableFrom(Class) is assignable from} the event's type.</p>
     *
     * <p>If there are no applicable subscribers for an event, it is considered "dead". Dead events are sometimes an
     * indication for a misconfiguration and can be caught by setting up a
     * {@link EventBus.Builder#setDeadEventHandler(Consumer) special handler}.</p>
     *
     * <p>The subscribers are called in no particular order, some time in the future and no guarantees are made that the
     * events are passed to the subscribers in the order in which they are posted to the bus. This behaviour may be
     * changed at the discretion of this bus' {@link EventDispatcher} and {@link Executor} implementation. However, it
     * is guaranteed, that an event will only be dispatched to the subscribers that were subscribed to this bus before
     * the event was posted.</p>
     *
     * @param <T>   the type of the event to post
     * @param event the event to be dispatched to this bus' subscribers
     *
     * @throws NullPointerException if the given event is {@code null}
     *
     * @since   1.0.0
     */
    public <T extends E> void post(T event) {
        Objects.requireNonNull(event);

        HashSet<Subscriber<? super T>> subscribers = new HashSet<>();

        @SuppressWarnings("unchecked")
        Class<? extends E> eventType = (Class<? extends E>) event.getClass();

        for (Class<? extends E> cls : withSuperTypes(eventType)) {
            CopyOnWriteArraySet<Subscriber<? extends E>> typeSubscribers = this.subscribers.get(cls);
            if (typeSubscribers == null) continue;

            List<Subscriber<? extends E>> invalidSubscribers = new ArrayList<>(0);

            for (Subscriber<? extends E> subscriber : typeSubscribers) {
                if (!subscriber.validate()) {
                    invalidSubscribers.add(subscriber);
                    continue;
                }

                @SuppressWarnings("unchecked")
                Subscriber<? super T> sub = (Subscriber<? super T>) subscriber;
                subscribers.add(sub);
            }

            typeSubscribers.removeAll(invalidSubscribers);
        }

        if (subscribers.isEmpty()) {
            if (this.deadEventHandler != null) this.deadEventHandler.accept(event);
        } else {
            this.dispatcher.dispatch(event, Collections.unmodifiableSet(subscribers));
            subscribers.forEach(Subscriber::invalidate);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends E>> withSuperTypes(Class<? extends E> type) {
        Set<Class<? extends E>> types = this.receivingTypes.get(type);
        if (types != null) return types;

        types = new HashSet<>();
        types.add(type);

        Class<?> superType = type.getSuperclass();
        if (superType != null && this.getEventType().isAssignableFrom(superType)) types.addAll(this.withSuperTypes((Class<? extends E>) superType));

        Class<?>[] superInterfaces = type.getInterfaces();
        for (Class<?> superInterface : superInterfaces) {
            if (this.getEventType().isAssignableFrom(superInterface)) types.addAll(this.withSuperTypes((Class<? extends E>) superInterface));
        }

        this.receivingTypes.put(type, Collections.unmodifiableSet(types));
        return types;
    }

    /**
     * A wrapper class for a handler subscribed to a bus.
     *
     * <p>This class provides several methods that serve as proxies to the underlying handler and may be used by a
     * {@link EventDispatcher dispatcher} to make decisions about the execution order.</p>
     *
     * @since   1.0.0
     */
    public static abstract class Subscriber<E> {

        protected final AtomicInteger refCount = new AtomicInteger(0);

        private final EventBus<? super E> bus;

        private Subscriber(EventBus<? super E> bus) {
            this.bus = bus;
        }

        /**
         * Dispatches the given event to the wrapped handler.
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
        public final void dispatch(E event) {
            Objects.requireNonNull(event);
            this.validate();

            this.bus.executor.execute(() -> {
                try {
                    this.invoke(event);
                } catch (Throwable t) {
                    if (this.bus.dispatchErrorHandler != null)
                        this.bus.dispatchErrorHandler.onDispatchError(event, this, t);
                } finally {
                    this.invalidate();
                }
            });
        }

        protected abstract void invoke(E event) throws Throwable;

        /**
         * If this subscriber was created via {@link EventBus#subscribe(Class, MethodHandles.Lookup) annotation-based
         * discovery}, this method serves as a proxy for {@link Method#getAnnotation(Class)}. Otherwise, this method
         * returns {@code null}.
         *
         * <p>This method should not be used outside of a {@link EventDispatcher#dispatch(Object, Set)} call.</p>
         *
         * @param <T>               the type of the annotation to query for and return if present
         * @param annotationClass   the {@code Class} object corresponding to the annotation type
         *
         * @return  the result of {@link Method#getAnnotation(Class)} if this subscriber represents a method, or
         *          {@code null} otherwise
         *
         * @throws NullPointerException if the given annotation class is {@code null}
         *
         * @since   1.0.0
         */
        @Nullable
        public abstract <T extends Annotation> T getAnnotation(Class<T> annotationClass);

        /**
         * If this subscriber was created via {@link EventBus#subscribe(Class, MethodHandles.Lookup) annotation-based
         * discovery}, this method serves as a proxy for {@link Method#getAnnotationsByType(Class)}. Otherwise, this
         * method returns {@code null}.
         *
         * <p>This method should not be used outside of a {@link EventDispatcher#dispatch(Object, Set)} call.</p>
         *
         * @param <T>               the type of the annotation to query for and return if present
         * @param annotationClass   the Class object corresponding to the annotation type
         *
         * @return  the result of {@link Method#getAnnotationsByType(Class)} if this subscriber represents a method, or
         *          {@code null} otherwise
         *
         * @throws NullPointerException if the given annotation class is {@code null}
         *
         * @since   1.0.0
         */
        @Nullable
        public abstract <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass);

        /**
         * Returns the {@link EventBus} that owns this subscriber.
         *
         * @return  the bus that owns this subscriber
         *
         * @since   1.1.0
         */
        public final EventBus<? super E> getBus() {
            return this.bus;
        }

        /**
         * Returns the origin of this subscriber.
         *
         * <p>The origin of a subscriber is the given {@code Class} or instance object when using
         * {@link EventBus#subscribe(Class, MethodHandles.Lookup) annotation-based discovery}, or {@code null} when
         * {@link EventBus#subscribe(Class, EventHandler) subscribing a handler manually.}</p>
         *
         * @return  the origin of this subscriber
         *
         * @since   1.1.0
         */
        @Nullable
        public abstract Object getOrigin();

        /**
         * If this subscriber was created via {@link EventBus#subscribe(Class, MethodHandles.Lookup) annotation-based
         * discovery}, this method serves as a proxy for {@link Method#isAnnotationPresent(Class)}. Otherwise, this
         * method returns {@code false}.
         *
         * <p>This method should not be used outside of a {@link EventDispatcher#dispatch(Object, Set)} call.</p>
         *
         * @param annotationClass   the Class object corresponding to the annotation type
         *
         * @return  the result of {@link Method#isAnnotationPresent(Class)} if this subscriber represents a method, or
         *          {@code false} otherwise
         *
         * @throws NullPointerException if the given annotation class is null
         *
         * @since   1.0.0
         */
        public abstract boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

        protected abstract boolean validate();
        protected abstract void invalidate();

        private static final class SubscribedEventHandler<E> extends Subscriber<E> {

            private final WeakReference<EventHandler<E>> ref;

            @Nullable
            private EventHandler<E> __handler;

            private SubscribedEventHandler(EventBus<? super E> bus, EventHandler<E> handler) {
                super(bus);
                this.ref = new WeakReference<>(handler);
            }

            @Override
            @Nullable
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            @Nullable
            public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
                return null;
            }

            @Override
            @Nullable
            public Object getOrigin() {
                return null;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            protected void invoke(E event) throws Throwable {
                Objects.requireNonNull(this.__handler).handle(event);
            }

            @Override
            protected boolean validate() {
                int res = this.refCount.getAndIncrement();
                if (res < 0) throw new IllegalStateException();

                if (res == 0) {
                    this.__handler = this.ref.get();
                    return (this.__handler != null);
                }

                return true;
            }

            @Override
            protected void invalidate() {
                int res = this.refCount.decrementAndGet();
                if (res < 0) throw new IllegalStateException();

                if (res == 0) {
                    this.__handler = null;
                }
            }

        }

        private static final class SubscribedMethodHandle<E> extends Subscriber<E> {

            private final WeakReference<Object> refOrigin;
            private final WeakReference<Method> refMethod;
            private final WeakReference<MethodHandle> refMethodHandle;

            @Nullable
            private MethodHandle __methodHandle;

            private SubscribedMethodHandle(EventBus<E> bus, Object origin, Method method, MethodHandle hMethod) {
                super(bus);
                this.refOrigin = new WeakReference<>(origin);
                this.refMethod = new WeakReference<>(method);
                this.refMethodHandle = new WeakReference<>(hMethod);
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                int res = this.refCount.getAndIncrement();
                if (res == 0) throw new IllegalStateException();

                try {
                    return Objects.requireNonNull(this.refMethod.get()).getAnnotation(annotationClass);
                } finally {
                    this.refCount.decrementAndGet();
                }
            }

            @Override
            public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
                int res = this.refCount.getAndIncrement();
                if (res == 0) throw new IllegalStateException();

                try {
                    return Objects.requireNonNull(this.refMethod.get()).getAnnotationsByType(annotationClass);
                } finally {
                    this.refCount.decrementAndGet();
                }
            }

            @Override
            public Object getOrigin() {
                int res = this.refCount.getAndIncrement();
                if (res == 0) throw new IllegalStateException();

                try {
                    return Objects.requireNonNull(this.refOrigin.get());
                } finally {
                    this.refCount.decrementAndGet();
                }
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                int res = this.refCount.getAndIncrement();
                if (res == 0) throw new IllegalStateException();

                try {
                    return Objects.requireNonNull(this.refMethod.get()).isAnnotationPresent(annotationClass);
                } finally {
                    this.refCount.decrementAndGet();
                }
            }

            @Override
            protected void invoke(E event) throws Throwable {
                Objects.requireNonNull(this.__methodHandle).invoke(event);
            }

            @Override
            protected boolean validate() {
                int res = this.refCount.getAndIncrement();
                if (res == Integer.MAX_VALUE) throw new IllegalStateException();

                if (res == 0) {
                    this.__methodHandle = this.refMethodHandle.get();
                    return (this.__methodHandle != null);
                }

                return true;
            }

            @Override
            protected void invalidate() {
                int res = this.refCount.decrementAndGet();
                if (res < 0) throw new IllegalStateException();

                if (res == 0) {
                    this.__methodHandle = null;
                }
            }

        }

    }

    /**
     * A builder class for an {@link EventBus}.
     *
     * <p>A builder is reusable. Calling {@linkplain #build()} does neither reset nor does it invalidate the builder.
     * </p>
     *
     * @since   1.0.0
     */
    public static final class Builder<E> {

        private final Class<E> type;

        private EventDispatcher<E> dispatcher;
        private Executor executor;
        private Class<? extends Annotation> subscriberMarker;
        private boolean isSelfCleaning;
        private Function<Class<?>, Method[]> mapper;

        @Nullable
        private DispatchErrorHandler<E> dispatchErrorHandler;

        @Nullable
        private Consumer<E> deadEventHandler;

        private Builder(Class<E> type) {
            this.type = type;
            this.dispatcher = EventDispatcher.perThreadDispatchQueue();
            this.executor = DefaultExecutors.directExecutor();
            this.subscriberMarker = EventSubscriber.class;
            this.isSelfCleaning = true;
            this.mapper = Class::getDeclaredMethods;
        }

        /**
         * Creates and returns a new bus with the settings currently defined in this builder.
         *
         * @return  a new bus with the settings as currently specified in this builder
         *
         * @since   1.0.0
         */
        public EventBus<E> build() {
            return new EventBus<>(this);
        }

        /**
         * Sets the {@link EventDispatcher dispatcher} implementation that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * @param dispatcher    the dispatcher for the bus
         *
         * <p>Defaults to {@link EventDispatcher#perThreadDispatchQueue()}.</p>
         *
         * @return  this builder instance
         *
         * @throws NullPointerException if the given dispatcher is {@code null}
         *
         * @since   1.0.0
         */
        public Builder<E> setDispatcher(EventDispatcher<E> dispatcher) {
            this.dispatcher = Objects.requireNonNull(dispatcher);
            return this;
        }

        /**
         * Sets the {@link DispatchErrorHandler} that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@code null}.</p>
         *
         * @param handler   the dispatch-error-handler for the bus
         *
         * @return  this builder instance
         *
         * @since   1.1.0
         */
        public Builder<E> setDispatchErrorHandler(@Nullable DispatchErrorHandler<E> handler) {
            this.dispatchErrorHandler = handler;
            return this;
        }

        /**
         * Sets the dead-event handler that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@code null}.</p>
         *
         * @param handler   the dead-event handler for the bus
         *
         * @return  this builder instance
         *
         * @since   2.0.0
         */
        public Builder<E> setDeadEventHandler(@Nullable Consumer<E> handler) {
            this.deadEventHandler = handler;
            return this;
        }

        /**
         * Sets the executor implementation that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@link DefaultExecutors#directExecutor()}.</p>
         *
         * @param executor  the executor for the bus
         *
         * @return  this builder instance
         *
         * @throws NullPointerException if the given executor is {@code null}
         *
         * @since   1.0.0
         */
        public Builder<E> setExecutor(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
            return this;
        }

        /**
         * Sets the function that the bus should use to extract methods from {@code Class} objects for
         * {@link #subscribe(Class, MethodHandles.Lookup) annotation-based subscriber discovery}.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@link Class#getDeclaredMethods()}.</p>
         *
         * @param mapper    the function to be used
         *
         * @return  this builder instance
         *
         * @see EventBus#subscribe(Class, MethodHandles.Lookup)
         * @see EventBus#subscribe(Object, MethodHandles.Lookup)
         *
         * @since   3.0.0
         */
        public Builder<E> setMethodMapper(Function<Class<?>, Method[]> mapper) {
            this.mapper = mapper;
            return this;
        }

        /**
         * Sets whether the bus will be self-cleaning.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@code true}.</p>
         *
         * @param value whether the bus should be self-cleaning
         *
         * @return  this builder instance
         *
         * @see EventBus#cleanup()
         *
         * @since   1.0.0
         */
        public Builder<E> setSelfCleaning(boolean value) {
            this.isSelfCleaning = value;
            return this;
        }

        /**
         * Sets the {@link EventBus#getSubscriberMarker() subscriber marker} that will be used by the bus.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@link EventSubscriber}.</p>
         *
         * @param type  the marker type
         *
         * @return  this builder instance
         *
         * @throws NullPointerException     if the given type is {@code null}
         * @throws IllegalArgumentException if the given type is not visible at runtime
         *
         * @see EventBus#subscribe(Class, MethodHandles.Lookup)
         * @see EventBus#subscribe(Object, MethodHandles.Lookup)
         *
         * @since   1.0.0
         */
        public Builder<E> setSubscriberMarker(Class<? extends Annotation> type) {
            Retention retention = Objects.requireNonNull(type).getAnnotation(Retention.class);

            if (retention == null || retention.value() != RetentionPolicy.RUNTIME)
                throw new IllegalArgumentException(type + " is not visible at runtime");

            this.subscriberMarker = Objects.requireNonNull(type);
            return this;
        }

    }

}