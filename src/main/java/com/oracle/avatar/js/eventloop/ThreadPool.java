/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.avatar.js.eventloop;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread pool executor.
 */
public final class ThreadPool extends ThreadPoolExecutor {
    private static final int DEFAULT_QUEUE_SIZE = Integer.MAX_VALUE;
    private static final int DEFAULT_CORE_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private static final int DEFAULT_MAX_THREADS = Integer.MAX_VALUE;
    private static final long DEFAULT_THREAD_TIMEOUT_SECONDS = 15;

    private static final String PACKAGE = ThreadPool.class.getPackage().getName() + ".";
    private static final String QUEUE_SIZE_PROPERTY = PACKAGE + "queueSize";
    private static final String CORE_THREAD_PROPERTY = PACKAGE + "coreThreads";
    private static final String MAX_THREADS_PROPERTY = PACKAGE + "maxThreads";
    private static final String THREAD_TIMEOUT_PROPERTY = PACKAGE + "threadTimeout";

    private final int taskQueueSize;
    private final BlockingQueue<Runnable> taskQueue;
    private final AtomicInteger activeTasks;

    /**
     * Returns a new default instance.
     *
     * @return The instance.
     */
    public static ThreadPool newInstance() {
        final int corePoolSize = Integer.getInteger(CORE_THREAD_PROPERTY, DEFAULT_CORE_THREADS);
        final int maximumPoolSize = Integer.getInteger(MAX_THREADS_PROPERTY, DEFAULT_MAX_THREADS);
        final long keepAliveTime = Long.getLong(THREAD_TIMEOUT_PROPERTY, DEFAULT_THREAD_TIMEOUT_SECONDS);
        final int taskQueueSize = Integer.getInteger(QUEUE_SIZE_PROPERTY, DEFAULT_QUEUE_SIZE);
        return newInstance(corePoolSize, maximumPoolSize, keepAliveTime, taskQueueSize);
    }

    /**
     * Returns a new instance.
     *
     * @param poolSize        The initial number of threads in the pool.
     * @param maximumPoolSize The maximum number of threads in the pool.
     * @param keepAliveTime   How long to keep threads beyond the initial threads alive.
     * @param taskQueueSize   The size of the task queue.
     * @return The instance.
     */
    public static ThreadPool newInstance(final int poolSize,
                                         final int maximumPoolSize,
                                         final long keepAliveTime,
                                         final int taskQueueSize) {
        final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(taskQueueSize);
        return new ThreadPool(poolSize, maximumPoolSize, keepAliveTime, taskQueueSize, taskQueue);
    }

    private ThreadPool(final int corePoolSize,
                       final int maximumPoolSize,
                       final long keepAliveTime,
                       final int taskQueueSize,
                       final BlockingQueue<Runnable> taskQueue) {
        super(corePoolSize,
              maximumPoolSize,
              keepAliveTime,
              TimeUnit.SECONDS,
              taskQueue,
              new DaemonThreadFactory("avatar-js.task"),
              new ThreadPoolExecutor.CallerRunsPolicy());
        this.taskQueueSize = taskQueueSize;
        this.taskQueue = taskQueue;
        this.activeTasks = new AtomicInteger(0);
    }

    /**
     * Returns {@code true} if there are active tasks.
     *
     * @return {@code true} if there are active tasks.
     */
    public boolean hasActiveTasks() {
        return activeTasks.get() != 0;
    }

    /**
     * Returns the number of active tasks.
     *
     * @return The count.
     */
    public int activeTaskCount() {
        return activeTasks.get();
    }

    /**
     * Returns the size of the task queue.
     *
     * @return The size.
     */
    public int taskQueueSize() {
        return taskQueueSize;
    }

    /**
     * Returns {@code true} if there are queued tasks.
     *
     * @return {@code true} if there are queued tasks.
     */
    public boolean hasQueuedTasks() {
        return taskQueue.peek() != null;
    }

    /**
     * Returns the number of queued tasks.
     *
     * @return The count.
     */
    public int queuedTasksCount() {
        return taskQueue.size();
    }

    /**
     * Returns the remaining capacity for queued tasks.
     *
     * @return The remaining capacity.
     */
    public int queuedTasksRemainingCapacity() {
        return taskQueue.remainingCapacity();
    }

    /**
     * Clears all queued tasks.
     */
    public void clearQueuedTasks() {
        taskQueue.clear();
    }

    /**
     * Returns a string describing the configuration.
     *
     * @return The description.
     */
    public String describeConfig() {
        return QUEUE_SIZE_PROPERTY + "=" + taskQueueSize() + ", " +
                CORE_THREAD_PROPERTY + "=" + getCorePoolSize() + ", " +
                MAX_THREADS_PROPERTY + "=" + getMaximumPoolSize();
    }

    @Override
    protected void beforeExecute(final Thread t, final Runnable r) {
        activeTasks.incrementAndGet();
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(final Runnable r, final Throwable t) {
        super.afterExecute(r, t);
        activeTasks.decrementAndGet();
    }
}
