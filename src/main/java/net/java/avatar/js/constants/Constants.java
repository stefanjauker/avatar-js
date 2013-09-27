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

package net.java.avatar.js.constants;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.java.avatar.js.Server;

public final class Constants {

    private static final Field[] FIELDS = Constants.class.getDeclaredFields();
    private static final int MASK = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

    private static final Map<String, Object> CONSTANTS;
    private static final Map<Object, String> CONSTANTS_STRING;

    static {
        CONSTANTS = new HashMap<>(FIELDS.length);
        CONSTANTS_STRING = new HashMap<>(FIELDS.length);
        for (final Field f : FIELDS) {
            if ((f.getModifiers() & MASK) == MASK) {
                try {
                    CONSTANTS.put(f.getName(), f.get(null));
                    CONSTANTS_STRING.put(f.get(null), f.getName());
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    // Should never happen, ignore with msg
                    if (Server.assertions()) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(CONSTANTS);
    }

    public static Map<Object, String> getConstantsString() {
        return Collections.unmodifiableMap(CONSTANTS_STRING);
    }

    public static final int O_RDONLY        = 0;
    public static final int O_WRONLY        = 1;
    public static final int O_RDWR          = 2;

    public static final int O_APPEND        = 0x0008;
    public static final int O_CREAT         = 0x0200;
    public static final int O_TRUNC         = 0x0400;
    public static final int O_EXCL          = 0x0800;
    public static final int O_SYNC          = 0x2000;
    public static final int O_NOCTTY        = 0x8000;

    public static final int S_IRUSR         = 0000400;
    public static final int S_IWUSR         = 0000200;
    public static final int S_IXUSR         = 0000100;
    public static final int S_IRWXU         = S_IRUSR | S_IWUSR | S_IXUSR;

    public static final int S_IRGRP         = 0000040;
    public static final int S_IWGRP         = 0000020;
    public static final int S_IXGRP         = 0000010;
    public static final int S_IRWXG         = S_IRGRP | S_IWGRP | S_IXGRP;

    public static final int S_IROTH         = 0000004;
    public static final int S_IWOTH         = 0000002;
    public static final int S_IXOTH         = 0000001;
    public static final int S_IRWXO         = S_IROTH | S_IWOTH | S_IXOTH;

    public static final int S_IFMT          = 0170000;
    public static final int S_IFIFO         = 0010000;
    public static final int S_IFCHR         = 0020000;
    public static final int S_IFDIR         = 0040000;
    public static final int S_IFBLK         = 0060000;
    public static final int S_IFREG         = 0100000;
    public static final int S_IFLNK         = 0120000;
    public static final int S_IFSOCK        = 0140000;
    public static final int S_IFWHT         = 0160000;

    public static final int S_ISUID         = 0004000;
    public static final int S_ISGID         = 0002000;
    public static final int S_ISVTX         = 0001000;

    public static final int SIGHUP          = 1;
    public static final int SIGINT          = 2;
    public static final int SIGQUIT         = 3;
    public static final int SIGILL          = 4;
    public static final int SIGTRAP         = 5;
    public static final int SIGABRT         = 6;
    public static final int SIGIOT          = 6;
    public static final int SIGBUS          = 7;
    public static final int SIGFPE          = 8;
    public static final int SIGKILL         = 9;
    public static final int SIGUSR1         = 10;
    public static final int SIGSEGV         = 11;
    public static final int SIGUSR2         = 12;
    public static final int SIGPIPE         = 13;
    public static final int SIGALRM         = 14;
    public static final int SIGTERM         = 15;
    public static final int SIGSTKFLT       = 16;
    public static final int SIGCHLD         = 17;
    public static final int SIGCONT         = 18;
    public static final int SIGSTOP         = 19;
    public static final int SIGTSTP         = 20;
    public static final int SIGTTIN         = 21;
    public static final int SIGTTOU         = 22;
    public static final int SIGURG          = 23;
    public static final int SIGXCPU         = 24;
    public static final int SIGXFSZ         = 25;
    public static final int SIGVTALRM       = 26;
    public static final int SIGPROF         = 27;
    public static final int SIGWINCH        = 28;
    public static final int SIGIO           = 29;
    public static final int SIGPOLL         = SIGIO;
    public static final int SIGPWR          = 30;
    public static final int SIGSYS          = 31;
    public static final int SIGUNUSED       = 31;

}
