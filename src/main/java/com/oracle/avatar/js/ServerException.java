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

package com.oracle.avatar.js;

import java.io.PrintStream;
import jdk.nashorn.api.scripting.NashornException;
import com.oracle.libuv.NativeException;

/**
 * An exception that filter-out internal frames of the original Exception.
 * Currently we have 3 possible types of Exception: 1) NashornException (direct
 * or cause of a ScriptException) that wraps any Exception thrown from
 * JavScript. 2) A SecurityException thrown from LibUV java layer. 3) Some Java
 * Exceptions thrown by core modules written in Java (Crypto/Zlib). 2) and 3)
 * are not filtered out. The cause of this exception is the originally thrown
 * exception.
 */
public final class ServerException extends Throwable {

    private static final long serialVersionUID = 8930713476760671228L;

    private final Throwable orig;
    private final StackTraceElement[] elems;
    private final NashornException nex;

    ServerException(final Throwable orig) {
        // No LibUV NativeException should be received,
        // these are translated by wrap scripts onto native JS Error.
        assert !(orig instanceof NativeException);

        this.orig = orig;
        nex = retrieveNashornException(orig);
        if (nex == null) {
            elems = orig.getStackTrace();
        } else {
            elems = NashornException.getScriptFrames(nex);
        }

        initCause(orig);
    }

    @Override
    public String getMessage() {
        return orig.getMessage();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return elems;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (nex == null) {
            orig.printStackTrace(s);
        } else {
            synchronized (s) {
                doPrintStackTrace(s, orig.getMessage(), getStackTrace());
                // Suppressed
                for (Throwable sup : orig.getSuppressed()) {
                    System.err.println("Suppressed...");
                    NashornException supNex = retrieveNashornException(sup);
                    if (supNex == null) {
                        sup.printStackTrace(s);
                    } else {
                        doPrintStackTrace(s, supNex.getMessage(),
                                NashornException.getScriptFrames(supNex));
                    }
                }
            }
        }
    }

    private static void doPrintStackTrace(PrintStream s, String message,
            StackTraceElement[] elements) {
        s.println(message);
        for (int i = 0; i < elements.length; i++) {
            StackTraceElement st = elements[i];
            s.print("\tat ");
            s.print(st.getMethodName());
            s.print(" (");
            s.print(st.getFileName());
            s.print(':');
            s.print(st.getLineNumber());
            s.println(")");
        }
    }

    private static NashornException retrieveNashornException(Throwable ex) {
        Throwable orig = ex;
        NashornException ret = null;

        while (orig != null && !(orig instanceof NashornException)) {
            orig = orig.getCause();
        }

        if (orig != null) {
            ret = (NashornException) orig;
        }
        return ret;
    }
}
