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

import javax.annotation.Nullable;

/**
 * A configuration that may be used to customize subscribers.
 *
 * @since   3.1.0
 *
 * @author  Leon Linhart
 */
public final class SubscriberConfig<E> {

    /**
     * {@return a builder for an {@code SubscriberConfig}}
     *
     * @param <E>   the type of the events handled by the subscriber
     *
     * @since   3.1.0
     */
    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    @Nullable
    final DispatchErrorHandler<E> dispatchErrorHandler;

    @Nullable
    final Object userData;

    private SubscriberConfig(Builder<E> builder) {
        this.dispatchErrorHandler = builder.dispatchErrorHandler;
        this.userData = builder.userData;
    }

    /**
     * A builder for a {@link SubscriberConfig}.
     *
     * @since   3.1.0
     */
    public static final class Builder<E> {

        @Nullable
        private DispatchErrorHandler<E> dispatchErrorHandler;

        @Nullable
        private Object userData;

        private Builder() {}

        /**
         * Creates and returns a new {@link SubscriberConfig subscriber configuration}.
         *
         * @return  a new {@link SubscriberConfig subscriber configuration}
         *
         * @since   3.1.0
         */
        public SubscriberConfig<E> build() {
            return new SubscriberConfig<>(this);
        }

        /**
         * Sets the {@link EventDispatcher dispatcher} implementation that will be used by the subscriber.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@code null}.</p>
         *
         * @param value the dispatch-error-handler for the subscriber
         *
         * @return  this builder instance
         *
         * @since   3.1.0
         */
        public Builder<E> withDispatchErrorHandler(@Nullable DispatchErrorHandler<E> value) {
            this.dispatchErrorHandler = value;
            return this;
        }

        /**
         * Sets a custom user-data object that will be referenced by the subscriber.
         *
         * <p><b>Consecutive calls overwrite the previously set value.</b></p>
         *
         * <p>Defaults to {@code null}.</p>
         *
         * @param value the arbitrary user-data for the subscriber
         *
         * @return  this builder instance
         *
         * @since   3.1.0
         */
        public Builder<E> withUserData(@Nullable Object value) {
            this.userData = value;
            return this;
        }

    }

}