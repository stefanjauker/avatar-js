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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.oracle.avatar.js.eventloop.EventLoop;
import com.oracle.avatar.js.eventloop.ThreadPool;
import com.oracle.avatar.js.log.Logging;

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
        final boolean[] didRun = new boolean[loops.length];
        for (int i=0; i < loops.length; i++) {
            didRun[i] = false;
            final EventLoop loop = loops[i] = new EventLoop(
                    properties.getProperty("source.compatible.version"),
                    properties.getProperty("libuv.compatible.version"),
                    logging,
                    System.getProperty("user.dir"),
                    i,
                    ThreadPool.getInstance(),
                    true);
            final int fi = i;
            try (EventLoop.Handle handle = loop.acquire()) {
                loop.submit(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("++ " + fi);
                        try {
                            Thread.sleep((long) (Math.random() * 1000));
                            didRun[fi] = true;
                        } catch (InterruptedException ex) {
                        } finally {
                            System.out.println("-- " + fi);
                        }
                    }
                });
            }
        }

        for (int i=0; i < loops.length; i++) {
            loops[i].run();
        }

        for (int i=0; i < loops.length; i++) {
            Assert.assertTrue(didRun[i]);
        }
    }

    public static void main(String[] args) throws Throwable {
        MultipleEventLoopTest test = new MultipleEventLoopTest();
        test.testSubmit();
    }
}
