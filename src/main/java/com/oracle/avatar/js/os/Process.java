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

package com.oracle.avatar.js.os;

public final class Process {

    private static final long NANOS = 1_000_000_000L;

    static {
        System.loadLibrary("avatar-js");
    }

    private Process() {}

    public static void abort() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("exitVM.*"));
        }
        _abort();
    }

    public static int getPid() {
        return _getPid();
    }

    public static int getUid() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.getuid"));
        }
        return _getUid();
    }

    public static void setUid(final int uid) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.setuid"));
        }
        _setUid(uid);
    }

    public static int getGid() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.getgid"));
        }
        return _getGid();
    }

    public static void setGid(final int gid) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.setgid"));
        }
        _setGid(gid);
    }

    public static int umask(final int mask) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.setumask"));
        }
        return _umask(mask);
    }

    public static int getUmask() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.getumask"));
        }
        return _getUmask();
    }

    public static int[] getGroups() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.getgroups"));
        }
        return _getGroups();
    }

    public static void setGroups(String[] grps) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.setgroups"));
        }
        _setGroups(grps);
    }

    public static void initGroups(String user, String group) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.initgroups"));
        }
        _initGroups_S_S(user, group);
    }

    public static void initGroups(int user, int group) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.initgroups"));
        }
        _initGroups_I_I(user, group);
    }

    public static void initGroups(int user, String group) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.initgroups"));
        }
        _initGroups_I_S(user, group);
    }

    public static void initGroups(String user, int group) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
           sm.checkPermission(new RuntimePermission("avatar-js.initgroups"));
        }
        _initGroups_S_I(user, group);
    }

    public static final class HRTime {

        public final long seconds;
        public final long nanoseconds;

        public HRTime(final long seconds, final long nanoseconds) {
            this.seconds = seconds;
            this.nanoseconds = nanoseconds;
        }

    }

    public static HRTime hrTime() {
        final long now = System.nanoTime();
        return new HRTime(now / NANOS, now % NANOS);
    }

    private static native void _abort();

    private static native int _getPid();

    private static native int _getUid();

    private static native void _setUid(int uid);

    private static native int _getGid();

    private static native void _setGid(int gid);

    private static native int _umask(int mask);

    private static native int _getUmask();

    private static native int[] _getGroups();

    private static native void _setGroups(String[] args);

    private static native void _initGroups_S_S(String user, String group);

    private static native void _initGroups_I_I(int user, int group);

    private static native void _initGroups_S_I(String user, int group);

    private static native void _initGroups_I_S(int user, String group);
}
