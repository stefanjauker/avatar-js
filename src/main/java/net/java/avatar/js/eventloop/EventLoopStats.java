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

package net.java.avatar.js.eventloop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public final class EventLoopStats implements EventLoopStatsMBean {

    private final Queue<Event> eventQueue;
    private final AtomicInteger hooks;
    private final BlockingQueue<Runnable> tasks;
    private final AtomicInteger activeTasks;
    private final ThreadPoolExecutor executor;

    public EventLoopStats(
        final Queue<Event> eventQueue,
        final AtomicInteger hooks,
        final BlockingQueue<Runnable> tasks,
        final AtomicInteger activeTasks,
        final ThreadPoolExecutor executor) {

        this.eventQueue = eventQueue;
        this.hooks = hooks;
        this.tasks = tasks;
        this.activeTasks = activeTasks;
        this.executor = executor;
    }

    @Override
    public int getHookCount() {
        return hooks.get();
    }

    @Override
    public int getScheduledTaskCount() {
        return tasks.size();
    }

    @Override
    public int getActiveTaskCount() {
        return activeTasks.get();
    }

    @Override
    public int getThreadCount() {
        return executor.getActiveCount();
    }

    @Override
    public int getPendingEventCount() {
        return eventQueue.size();
    }

    @Override
    public List<String> getPendingEvents() {
        final List<String> events = new ArrayList<>(eventQueue.size());
        for (final Event e : eventQueue) {
            events.add(e.toString());
        }
        return Collections.unmodifiableList(events);
    }

}
