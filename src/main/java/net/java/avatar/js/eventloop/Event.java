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

import java.security.AccessControlContext;
import java.security.AccessController;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import net.java.avatar.js.Server;

public final class Event {

    private final String name;
    private final Callback callback;
    private final Object[] args;
    private final AccessControlContext ctx;
    private final ScriptObjectMirror domain;

    public Event(final String name, final ScriptObjectMirror domain, final Callback callback) {
        this(name, domain, callback, (Object[]) null);
    }

    public Event(final String name, final ScriptObjectMirror domain, final Callback callback, final Object arg) {
        this.name = name;
        this.domain = domain;
        this.callback = callback;
        this.args = new Object[1];
        this.args[0] = arg;
        if(System.getSecurityManager() != null) {
           this.ctx = AccessController.getContext();
        } else {
            this.ctx = null;
        }
    }

    public Event(final String name, final Callback callback, final Object... args) {
        this.name = name;
        this.domain = null;
        this.callback = callback;
        this.args = args == null ? null : args.clone();
        if(System.getSecurityManager() != null) {
           this.ctx = AccessController.getContext();
        } else {
            this.ctx = null;
        }
    }

    AccessControlContext getContext() {
            return ctx;
    }

    String getName() {
        return name;
    }

    Object[] getArgs() {
        return args;
    }

    Callback getCallback() {
        return callback;
    }

    @Override
    public String toString() {
        return Event.toString(name, args);
    }

    public static String toString(final String name, final Object[] args) {
        final StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        // increase verboseness (by including args) if assertions are enabled
        if (args != null && Server.assertions()) {
            sb.append(" [");
            for (int i=0; i < args.length; i++) {
                final Object arg = args[i];
                if (arg != null) {
                    sb.append(arg.toString());
                }
                if (i + 1 < args.length) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public ScriptObjectMirror getDomain() {
        return domain;
    }
}
