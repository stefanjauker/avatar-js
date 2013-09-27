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

import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

final class TimersTask extends TimerTask {

    private final Timers timers;
    private final EventLoop eventLoop;
    private final long delay;
    private final boolean repeating;
    private final Callback callback;
    private final Timers.TimerHandle id;
    private final AtomicBoolean done = new AtomicBoolean(false);

    private ScheduledFuture<?> future = null;

    TimersTask(final Timers timers,
               final EventLoop eventLoop,
               final Timers.TimerHandle id,
               final long delay,
               final boolean repeating,
               final Callback callback) {
        this.timers = timers;
        this.eventLoop = eventLoop;
        this.delay = delay;
        this.repeating = repeating;
        this.callback = callback;
        this.id = id;
    }

    void start() {
        future = repeating ?
                timers.timer().scheduleAtFixedRate(this, delay, delay, TimeUnit.MILLISECONDS) :
                timers.timer().schedule(this, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        if (!repeating) {
            if (done.getAndSet(true)) {
                return;
            }
            // Need to post before to clear, otherwise EvtLoop can exit
            // after the timer has been removed.
            postEvent();
            timers.clearTimeout(id);
        } else {
            postEvent();
        }

    }

    private void postEvent() {
        final Callback guard = new Callback() {

            @Override
            public void call(String name, Object[] args) throws Exception {
                if (repeating && done.get()) {
                    //Already cleared.
                    return;
                }
                callback.call(name, args);
            }
        };
        eventLoop.post(new Event("timer"+id, guard));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof TimersTask) {
            return id == ((TimersTask) obj).id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.id;
    }

    @Override
    public boolean cancel() {
        done.set(true);
        if (future != null) {
            future.cancel(true);
        }
        return super.cancel();
    }
}
