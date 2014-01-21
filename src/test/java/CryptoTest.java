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

import com.oracle.avatar.js.Loader;
import java.io.File;
import com.oracle.avatar.js.Server;
import com.oracle.avatar.js.log.Logging;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptEngine;
import org.testng.annotations.Test;

/**
 * Test crypto.
 *
 */
public class CryptoTest {

    private static int p = 59152;
    private static final String SCRIPT_PORT = "SCRIPT_PORT";

    @Test
    public void testCrypto() throws Exception {
        File dir = new File("src/test/js/crypto");
        boolean failed = false;
        for (File f : dir.listFiles()) {
            final String[] args = { f.getAbsolutePath() };
            System.out.println("Running " + f.getAbsolutePath());
            try {
                Map<String, Object> bindings = new HashMap<>();
                bindings.put(SCRIPT_PORT, getPort());
                ScriptEngine engine = newEngine(bindings);
                newServer(engine);
                System.out.println(f + " test passed");
            } catch(Exception ex) {
                System.out.println(f + " test failure");
                ex.printStackTrace();
                failed = true;
            }
        }
        if (failed) {
            throw new Exception("Crypto test failed");
        }
    }

    private static int getPort() {
        return ++p;
    }

    private static ScriptEngine newEngine(Map<String, Object> b) throws Exception {
        ScriptEngine engine = Server.newEngine();
        for (String k : b.keySet()) {
            engine.put(k, b.get(k));
        }
        return engine;
    }

    private static Server newServer(ScriptEngine engine) throws Exception {
        Server server = new Server(engine, new Loader.Core(), new Logging(false),
                System.getProperty("user.dir"));
        return server;
    }
}
