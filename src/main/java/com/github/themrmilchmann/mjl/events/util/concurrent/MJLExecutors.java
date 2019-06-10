/*
 * Copyright 2018-2019 Leon Linhart
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
package com.github.themrmilchmann.mjl.events.util.concurrent;

import java.util.concurrent.Executor;

/**
 * A collection of executors that are commonly used with an EventBus.
 *
 * @since   1.0.0
 *
 * @author  Leon Linhart
 */
public final class MJLExecutors {

    private MJLExecutors() {}

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
     * @since   1.0.0
     */
    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    private static final class DirectExecutor implements Executor {

        private static final Executor INSTANCE = new DirectExecutor();

        @Override
        public void execute(Runnable command) {
            command.run();
        }

    }

}