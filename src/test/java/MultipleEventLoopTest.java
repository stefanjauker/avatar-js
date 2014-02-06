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

import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.oracle.avatar.js.eventloop.EventLoop;
import com.oracle.avatar.js.eventloop.ThreadPool;
import com.oracle.avatar.js.log.Logging;
import com.oracle.libuv.cb.AsyncCallback;
import com.oracle.libuv.handles.AsyncHandle;

public class MultipleEventLoopTest {

    @Test
    public void testSubmit() throws Throwable {
        final Properties properties = new Properties();
        try (Reader reader = new FileReader("project.properties")) {
            properties.load(reader);
        }

        final int CONCURRENCY = 256;
        final Logging logging = new Logging(false);
        final EventLoop[] loops = new EventLoop[CONCURRENCY];
        final Thread[] threads = new Thread[loops.length];
        final AtomicBoolean[] initialized = new AtomicBoolean[loops.length];
        final AtomicBoolean[] onAsync = new AtomicBoolean[loops.length];
        final AsyncHandle[] asyncHandles = new AsyncHandle[loops.length];
        final Throwable[] exceptions = new Throwable[loops.length];

        for (int i=0; i < loops.length; i++) {
            initialized[i] = new AtomicBoolean(false);
            onAsync[i] = new AtomicBoolean(false);
            exceptions[i] = null;
        }

        for (int i=0; i < loops.length; i++) {
            final int fi = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final EventLoop loop = loops[fi] = new EventLoop(
                                properties.getProperty("source.compatible.version"),
                                properties.getProperty("libuv.compatible.version"),
                                logging,
                                System.getProperty("user.dir"),
                                fi,
                                ThreadPool.newInstance(1, 1, 1, Integer.MAX_VALUE),
                                false);

                        final AsyncHandle async = asyncHandles[fi] = new AsyncHandle(loop.loop());
                        async.setAsyncCallback(new AsyncCallback() {
                            @Override
                            public void onSend(int status) throws Exception {
                                onAsync[fi].set(true);
                                async.unref();
                            }
                        });

                        final AtomicBoolean init = initialized[fi];
                        synchronized (init) {
                            init.set(true);
                            init.notifyAll();
                        }

                        loop.run();
                    } catch (Throwable ex) {
                        exceptions[fi] = ex;
                    }
                }
            });

            threads[i].start();
        }

        for (int i=0; i < loops.length; i++) {
            final int fi = i;
            final AtomicBoolean init = initialized[fi];
            synchronized (init) {
                while (!init.get()) {
                    init.wait();
                }
            }

            final EventLoop loop = loops[fi];
            try (EventLoop.Handle handle = loop.acquire()) {
                loop.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100 + (long) (Math.random() * 1000));
                        } catch (Throwable th) {
                            exceptions[fi] = th;
                        }
                    }
                });
            }
        }

        for (int i=0; i < loops.length; i++) {
            asyncHandles[i].send();
        }

        for (int i=0; i < loops.length; i++) {
            threads[i].join();
            final Throwable th = exceptions[i];
            if (th != null) {
                throw new AssertionError(th);
            }
            assert onAsync[i].get();
        }
    }

    public static void main(String[] args) throws Throwable {
        MultipleEventLoopTest test = new MultipleEventLoopTest();
        test.testSubmit();
    }
}
