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

package net.java.avatar.js.timers;

import net.java.libuv.Callback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import net.java.avatar.js.eventloop.DaemonThreadFactory;
import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

public final class Timers {

    public final class TimerHandle {

        final int id;
        int referenceCount;

        TimerHandle(final int id) {
            this.id = id;
            this.referenceCount = 1;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof TimerHandle) {
                final TimerHandle timerHandle = (TimerHandle) other;
                return id == timerHandle.id;
            }
            return false;
        }

        @Override
        public String toString() {
            return "{name:" + TimerHandle.class.getSimpleName() +
                    ", id:" + Integer.toString(id) +
                    ", ref:" + Integer.toString(referenceCount) + "}";
        }

        public void ref() {
            referenceCount++;
        }

        public TimerHandle unref() {
            referenceCount--;
            return this;
        }

        public boolean isReferenced() {
            return referenceCount > 0;
        }
    }

    final class Immediate implements Callback {

        private final Callback cb;
        private boolean cleared;

        Immediate(final Callback cb) {
            this.cb = cb;
        }

        void clear() {
            cleared = true;
        }

        @Override
        public void call(String name, Object[] args) throws Exception {
            if (!cleared) {
                cb.call(name, args);
            }
        }
    }

    private final EventLoop eventLoop;
    private final AtomicInteger NEXT_ID = new AtomicInteger();
    private final Map<TimerHandle, TimersTask> SCHEDULED = new ConcurrentHashMap<>();
    private final Map<TimerHandle, Immediate> IMMEDIATE = new ConcurrentHashMap<>();
    private final ScheduledExecutorService TIMER =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("avatar-js.timer"));

    public Timers(final EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public TimerHandle setTimeout(final boolean repeating,
                                  final long delay,
                                  final Callback callback) {
        // adjust zero or negative (invalid) delay values to the smallest positive delay
        final long positiveDelay = delay > 0 ? delay : 1;
        final TimerHandle id = new TimerHandle(NEXT_ID.incrementAndGet());
        final TimersTask task = new TimersTask(this, eventLoop, id, positiveDelay, repeating, callback);
        SCHEDULED.put(id, task);
        return id;
    }

    public void start(final TimerHandle id) {
        final TimersTask task = SCHEDULED.get(id);
        if (task != null) {
            task.start();
        }
    }

    public void clearTimeout(final TimerHandle timeoutId) {
        final TimersTask task = SCHEDULED.remove(timeoutId);
        if (task != null) {
            task.cancel();
        }
    }

    public void clearAll() {
        for (final TimersTask task : SCHEDULED.values()) {
            task.cancel();
        }
        SCHEDULED.clear();
    }

    public boolean isPending() {
        for (final Map.Entry<TimerHandle, TimersTask> entry : SCHEDULED.entrySet()) {
            if (entry.getKey().isReferenced()) {
                return true;
            }
        }
        return false;
    }

    public TimerHandle setImmediate(final Callback callback) {
        final Immediate immediate = new Immediate(callback);
        final TimerHandle id = new TimerHandle(NEXT_ID.incrementAndGet());
        IMMEDIATE.put(id, immediate);
        eventLoop.post(new Event("timer.immediate." + id, immediate));
        return id;
    }

    public void clearImmediate(final TimerHandle immediateId) {
        final Immediate task = IMMEDIATE.remove(immediateId);
        if (task != null) {
            task.clear();
        }
    }

    ScheduledExecutorService timer() {
        return TIMER;
    }
}
