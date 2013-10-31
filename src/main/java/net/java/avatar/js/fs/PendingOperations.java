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

package net.java.avatar.js.fs;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

// used only in JavaScript, so not synchronized
// values are FIFO, duplicates allowed
@SuppressWarnings("unused")
public final class PendingOperations {

    // multimap
    private final Map<Integer, Queue<Object>> map = new HashMap<>();

    public int push(final Object callback) {
        final int id = callback.hashCode();
        Queue<Object> bucket = map.get(id);
        if (bucket == null) {
            bucket = new ArrayDeque<>();
            map.put(id, bucket);
        }
        bucket.offer(callback);
        return id;
    }

    public Object shift(final int id) {
        final Queue<Object> bucket = map.get(id);
        assert bucket.size() > 0;
        final Object callback = bucket.poll();
        assert callback != null;
        if (bucket.size() == 0) {
            map.remove(id);
        }
        return callback;
    }
}
