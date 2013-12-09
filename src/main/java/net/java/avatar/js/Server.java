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

package net.java.avatar.js;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.NashornException;

import net.java.avatar.js.eventloop.EventLoop;
import net.java.avatar.js.eventloop.ThreadPool;
import net.java.avatar.js.log.Logger;
import net.java.avatar.js.log.Logging;

import jdk.nashorn.api.scripting.URLReader;

/**
 * Node server.
 */
public final class Server {

    private static final String PACKAGE = "/net/java/avatar/js";
    private static final String HELP =
            "Usage: java -jar avatar-js.jar [options] [ script.js ] [arguments]\n" +
            "\n" +
            "Options:\n" +
            "  -v, --version        print version\n" +
            "  --no-deprecation     silence deprecation warnings\n" +
            "  --trace-deprecation  trace deprecation warnings\n" +
            "\n";

    private final SystemScriptRunner[] SYSTEM_INIT_SCRIPTS = {
            new InitScriptRunner()
    };

    private final SystemScriptRunner[] SYSTEM_FINALIZATION_SCRIPTS = {
            new FinalScriptRunner()
    };

    private static final String ENGINE_NAME = System.getProperty("avatar.scriptEngine", "nashorn");
    private static final ScriptEngineManager MANAGER = new ScriptEngineManager();

    private static final String LOG_OUTPUT_DIR = "avatar-js.log.output.dir";
    private static final String VERSION_BUILD_PROPERTY = "avatar-js.source.compatible.version";
    private static final String LIBUV_VERSION_BUILD_PROPERTY = "avatar-js.libuv.compatible.version";
    private static final String SECURE_HOLDER = "__avatar";

    private static boolean assertions = false;

    private final ScriptEngine engine;
    private final ScriptContext context;
    private final Bindings bindings;
    private final EventLoop eventLoop;
    private final Logging logging;
    private final Logger log;
    private final SecureHolder holder;

    private final String version;
    private final String uvVersion;
    private final boolean rethrowException;
    static {
        initAssertionStatus();
    }

    public static void main(final String... args) throws Exception {
        new Server(false).run(args);
    }

    public Server(boolean rethrowException) throws Exception {
        this(newEngine(),
                new Loader.Core(),
                System.getProperty(LOG_OUTPUT_DIR) == null ?
                        new Logging(assertions) :
                        new Logging(new File(System.getProperty(LOG_OUTPUT_DIR)), assertions),
                System.getProperty("user.dir"), rethrowException);
    }

    public Server(final ScriptEngine engine,
                  final Loader loader,
                  final Logging logging,
                  final String workDir,
                  final boolean rethrowException) throws Exception {
        this(engine, loader, logging, workDir, engine.getContext(), 0, ThreadPool.getInstance(), rethrowException);
    }

    public Server(final ScriptEngine engine,
                  final Loader loader,
                  final Logging logging,
                  final String workDir,
                  final ScriptContext context,
                  final int instanceNumber,
                  final ThreadPool threadPool,
                  final boolean rethrowException) throws Exception {
        this.engine = engine;
        this.context = context;
        this.bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        this.logging = logging;
        this.log = logging.getDefault(); // server-wide log
        version = loader.getBuildProperty(VERSION_BUILD_PROPERTY);
        assert version != null;
        uvVersion = loader.getBuildProperty(LIBUV_VERSION_BUILD_PROPERTY);
        assert uvVersion != null;
        this.eventLoop = new EventLoop(version, uvVersion, logging, workDir, instanceNumber, threadPool);
        this.holder = new SecureHolder(eventLoop, loader, (Invocable) engine);
        this.rethrowException = rethrowException;
    }

    public void run(final String... args) throws Exception {
        // No Server instance can be accessed from user scripts.
        // Although this public method is not accessible, do a permission check.
        checkPermission();
        bindings.put(SECURE_HOLDER, holder);
        try {
            if (args.length == 0) {
                runREPL();
            } else {
                runUserScripts(args);
            }
        } catch (final Exception ex) {
            if (!eventLoop.handleCallbackException(ex)) {
                if (rethrowException) {
                    throw ex;
                } else {
                     NashornException nex = retrieveNashornException(ex);
                     if (nex != null) {
                         System.err.println(formatException(nex, 0));
                         for (Throwable sup : ex.getSuppressed()) {
                             System.err.println("Suppressed...");
                             NashornException supNex = retrieveNashornException(sup);
                             if (supNex != null) {
                                 System.err.println(formatException(supNex, 0));
                             } else {
                                 supNex.printStackTrace(System.err);
                             }
                         }
                     } else {
                         ex.printStackTrace(System.err);
                     }
                 }
            }
        } finally {
            eventLoop.stop();
            logging.shutdown();
        }
    }

    public static String formatException(Throwable ex, int startIndex) {
        if (ex == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(ex.toString()).append("\n");
        StackTraceElement[] elems = NashornException.getScriptFrames(ex);

        for (int i = startIndex; i < elems.length; i++) {
            StackTraceElement st = elems[i];
            builder.append("\tat ");
            builder.append(st.getMethodName());
            builder.append(" (");
            builder.append(st.getFileName());
            builder.append(':');
            builder.append(st.getLineNumber());
            builder.append(")");
            if (i != elems.length - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private static NashornException retrieveNashornException(Throwable ex) {
        Throwable orig = ex;
        NashornException ret = null;
        while (!(orig instanceof NashornException)) {
            orig = ex.getCause();
        }
        if (orig != null) {
            ret = (NashornException) orig;
        }
        return ret;
    }

    public void interrupt() {
        eventLoop.stop();
    }

    private void runSystemScript(final SystemScriptRunner... scripts)
            throws FileNotFoundException, ScriptException {
        for (final SystemScriptRunner scriptRunner : scripts) {
            log.log("loading system script " + scriptRunner.script);
            scriptRunner.run(context);
        }
    }

    private void runUserScripts(final String... args) throws Exception {
        String userFile = null;
        final List<String> userArgs = new ArrayList<>();
        final List<String> avatarArgs = new ArrayList<>();

        if (args != null && args.length > 0) {
            for (int i=0; i < args.length; i++) {
                if (userFile == null) {
                    if (args[i].startsWith("-")) {
                        avatarArgs.add(args[i]);
                    } else {
                        userFile = args[i];
                    }
                } else {
                    userArgs.add(args[i]);
                }
            }

            if (userFile != null) {
                final Path p = Paths.get(userFile);
                // prefix with "./" if not absolute
                userFile = p.isAbsolute() ? p.toString() : Paths.get(".", p.toString()).toString();
            }
        }

        log.log("avatar args " + avatarArgs);
        log.log("user file " + userFile);
        log.log("user args " + userArgs);

        processArgs(avatarArgs);

        if (userFile == null) {
            return;
        }

        final String[] userFiles = {userFile};

        runEventLoop(avatarArgs.toArray(new String[avatarArgs.size()]),
                     userArgs.toArray(new String[userArgs.size()]),
                     userFiles);
    }

    private void runREPL() throws Exception {
        holder.setForceRepl(true);
        runEventLoop(null, null, null);
    }

    private void runEventLoop(final String[] avatarArgs, final String[] userArgs, final String[] userFiles) throws Exception {
        Exception rootCause = null;
        holder.setArgs(avatarArgs, userArgs, userFiles);

        try {
            runSystemScript(SYSTEM_INIT_SCRIPTS);
        } catch(Exception ex) {
            if (!eventLoop.handleCallbackException(ex)) {
                rootCause = ex;
                throw ex;
            }
        } finally {
            if (rootCause != null) {
                try {
                    // emit the process.exit event
                    runSystemScript(SYSTEM_FINALIZATION_SCRIPTS);
                } catch (Exception ex) {
                    if (!eventLoop.handleCallbackException(ex)) {
                        rootCause.addSuppressed(ex);
                        throw rootCause;
                    }
                }
            }
        }
        // ...then run the main event loop. If an exception has been handled
        // the process can continue. For example some timer events can be fired.
        try {
            eventLoop.run();
        } catch(Exception ex) {
            boolean rethrow = false;
            if (!eventLoop.handleCallbackException(ex)) {
                rethrow = true;
                eventLoop.stop();
            }
            holder.setExitCode(1);
            if (rethrow) {
                rootCause = ex;
                throw ex;
            }
        } finally {
            try {
                // emit the process.exit event
                runSystemScript(SYSTEM_FINALIZATION_SCRIPTS);
            } catch (Exception ex) {
                if (rootCause != null) {
                    rootCause.addSuppressed(ex);
                    throw rootCause;
                }
                throw ex;
            }
        }
    }

    private void processArgs(final List<String> args) {
        for (int i=0; i < args.size(); i++) {
            final String arg = args.get(i);
            if ("-h".equals(arg) || "--help".equals(arg)) {
                System.out.println(HELP);
                System.exit(0);
            } else if ("-v".equals(arg) || "--version".equals(arg)) {
                System.out.println("v" + version);
                System.exit(0);
            } else if ("-uv".equals(arg) || "--uv-version".equals(arg)) {
                System.out.println("v" + uvVersion);
                System.exit(0);
            } else if ("--no-deprecation".equals(arg)) {
                holder.setThrowDeprecation(false);
            } else if ("--trace-deprecation".equals(arg)) {
                holder.setTraceDeprecation(true);
                holder.setThrowDeprecation(false);
            } else if ("-i".equals(arg) || "--interactive".equals(arg)) {
                holder.setForceRepl(true);
            } else {
                System.out.println(HELP);
                throw new IllegalArgumentException(arg);
            }
        }
    }

    private Object eval(final String fileName, final URL url,
            final ScriptContext context) throws FileNotFoundException, ScriptException {
        assert fileName != null;
        if (url == null) {
            throw new FileNotFoundException(fileName);
        }
        assert bindings != null;
        bindings.put(ScriptEngine.FILENAME, fileName);
        return context == null ?
            engine.eval(new URLReader(url)) :
            engine.eval(new URLReader(url), context);
    }

    public static ScriptEngine newEngine() {
        checkPermission();
        return MANAGER.getEngineByName(ENGINE_NAME);
    }

    public static CompiledScript compile(final ScriptEngine engine, final String script) throws ScriptException {
        return ((Compilable) engine).compile(script);
    }

    public static boolean assertions() {
        return assertions;
    }

    public static URL getResource(final String path) {
        return Server.class.getResource(path);
    }

    /*
     * Called by process.js when returning env map.
     */
    public static void checkGetEnv() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // This is checking for the right permission
            System.getenv();
        }
    }

    public EventLoop eventLoop() {
        return holder.getEventloop();
    }

    public Logger logger() {
        return log;
    }

    private static void checkPermission() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            final RuntimePermission perm = new RuntimePermission("avatar-js");
            sm.checkPermission(perm);
        }
    }

    public final class SecureHolder {

        private final EventLoop evtloop;
        private final Loader loader;
        private String[] avatarArgs;
        private String[] userArgs;
        private String[] userFiles;
        private boolean throwDeprecation = true;
        private boolean traceDeprecation;
        private boolean forceRepl = false;
        private int exitCode = 0;
        private final Invocable invocable;
        private Object nativeModule;
        private final AccessControlContext ctx;

        private SecureHolder(final EventLoop evtloop,
                             final Loader loader,
                             final Invocable invocable) {
            this.evtloop = evtloop;
            this.loader = loader;
            this.invocable = invocable;
            this.ctx = AccessController.getContext();
        }

        public AccessControlContext getControlContext() {
            checkPermission();
            return ctx;
        }

        public EventLoop getEventloop() {
            checkPermission();
            return evtloop;
        }

        public Loader getLoader() {
            checkPermission();
            return loader;
        }

        private void setArgs(final String[] avatarArgs, final String[] userArgs, final String[] userFiles) {
            this.avatarArgs = avatarArgs != null ? avatarArgs.clone() : null;
            this.userArgs = userArgs != null ? userArgs.clone() : null;
            this.userFiles = userFiles != null ? userFiles.clone() : null;
        }

        public String[] getAvatarArgs() {
            return avatarArgs;
        }

        public String[] getUserArgs() {
            return userArgs;
        }

        public String[] getUserFiles() {
            return userFiles;
        }

        private void setThrowDeprecation(boolean throwDeprecation) {
            this.throwDeprecation = throwDeprecation;
        }

        private void setTraceDeprecation(boolean traceDeprecation) {
            this.traceDeprecation = traceDeprecation;
        }

        private void setForceRepl(boolean forceRepl) {
            this.forceRepl = forceRepl;
        }

        public boolean getThrowDeprecation() {
            return throwDeprecation;
        }

        public boolean getTraceDeprecation() {
            return traceDeprecation;
        }

        public boolean getForceRepl() {
            return forceRepl;
        }

        private void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }

        public void installNativeModule(Object nativeModule) {
            checkPermission();
            if(this.nativeModule != null) {
                throw new RuntimeException("NativeModule already set");
            }
            this.nativeModule = nativeModule;
        }

        /**
         * This is an equivalent of javascript require("<native module name>")
         * targeted for Java code.
         */
        public Object require(String moduleName) throws Exception {
            return invocable.invokeMethod(nativeModule, "require", moduleName);
        }
    }

    private abstract class SystemScriptRunner {

        private final String script;

        protected SystemScriptRunner(final String script) {
            this.script = script;
        }

        /*
         * Run the script in the specified context
         */
        protected Object run(final ScriptContext context) throws ScriptException, FileNotFoundException {
            return eval(script, Server.class.getResource(script), context);
        }
    }

    private final class InitScriptRunner extends SystemScriptRunner {
        private InitScriptRunner() {
            super(PACKAGE + "/init.js");
        }
    }

    private final class FinalScriptRunner extends SystemScriptRunner {
        private FinalScriptRunner() {
            super(PACKAGE + "/final.js");
        }
    }

    @SuppressWarnings("all")
    private static void initAssertionStatus() {
        assert assertions = true; // intentional side-effect
    }

    private static String parent(final String fileName) {
        final int lastslash = fileName.lastIndexOf('/');
        return lastslash > 0 ? fileName.substring(0, lastslash) : "/";
    }
}
