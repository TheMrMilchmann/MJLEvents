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

import java.util.concurrent.Executor;

/**
 * A collection of useful executors for use in an {@link EventBus}.
 *
 * @since   3.0.0
 *
 * @author  Leon Linhart
 */
public final class DefaultExecutors {

    /**
     * Returns an {@link Executor} that runs each task directly in the thread that invokes
     * {@link Executor#execute(Runnable)}.
     *
     * <p>The implementation is equivalent to:</p>
     *
     * <pre>{@code
     * class DirectExecutor implements Executor {
     *
     *     public void execute(Runnable task) {
     *         task.run();
     *     }
     *
     * }
     * }</pre>
     *
     * @return  an {@link Executor} that runs each task directly in the thread that invokes
     *          {@link Executor#execute(Runnable)}
     *
     * @since   3.0.0
     */
    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    @Deprecated
    private DefaultExecutors() { throw new UnsupportedOperationException(); }

    private static final class DirectExecutor implements Executor {

        private static final Executor INSTANCE = new DirectExecutor();

        @Override
        public void execute(Runnable command) {
            command.run();
        }

    }

}