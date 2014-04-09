/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the GNU General
 * Public License Version 2 only ("GPL"). You may not use this file except
 * in compliance with the License.  You can obtain a copy of the License at
 * https://avatar.java.net/license.html or legal/LICENSE.txt.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.avatar.js;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.oracle.avatar.js.eventloop.Callback;
import com.oracle.avatar.js.eventloop.EventLoop;
import com.oracle.avatar.js.eventloop.ThreadPool;
import com.oracle.avatar.js.log.Logger;
import com.oracle.avatar.js.log.Logging;
import com.oracle.libuv.LibUV;
import com.oracle.libuv.cb.AsyncCallback;
import com.oracle.libuv.handles.AsyncHandle;
import com.oracle.libuv.handles.DefaultHandleFactory;
import com.oracle.libuv.handles.HandleFactory;

import jdk.nashorn.api.scripting.URLReader;

/**
 * Avatar.js server.
 */
public final class Server implements AutoCloseable {

    private static final String PACKAGE = "/com/oracle/avatar/js";
    private static final String HELP =
            "Usage: java -jar avatar-js.jar [options] [ script.js ] [arguments]\n" +
            "\n" +
            "Options:\n" +
            "  -v, --version        print version\n" +
            "  -uv, --uv-version    print uv version\n" +
            "  -i, --interactive    force repl\n" +
            "  -e, --eval script    evaluate script\n" +
            "  -p, --print          evaluate script and print result\n" +
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
    private static final String[] EMPTY_ARRAY = {};

    private static boolean assertions = false;

    private final ScriptEngine engine;
    private final ScriptContext context;
    private final Bindings bindings;
    private final EventLoop eventLoop;
    private final Logging logging;
    private final Logger log;
    private final SecureHolder holder;
    private final AsyncHandle keepAlive;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Callback listener;

    private final String version;
    private final String uvVersion;

    private final List<String> userArgs = new ArrayList<>();
    private final List<String> avatarArgs = new ArrayList<>();
    private String userFile = null;

    static {
        initAssertionStatus();
    }

    public static void main(final String... args) throws Throwable {
        new Server().run(args);
    }

    public Server() throws Exception {
        this(newEngine(),
                new Loader.Core(),
                System.getProperty(LOG_OUTPUT_DIR) == null ?
                        new Logging(assertions) :
                        new Logging(new File(System.getProperty(LOG_OUTPUT_DIR)), assertions),
                System.getProperty("user.dir"));
    }

    public Server(final ScriptEngine engine,
                  final Loader loader,
                  final Logging logging,
                  final String workDir) throws Exception {
        this(engine, loader, logging, workDir, engine.getContext(), 0, ThreadPool.newInstance(), null, null, false);
    }

    public Server(final ScriptEngine engine,
                  final Loader loader,
                  final Logging logging,
                  final String workDir,
                  final ScriptContext context,
                  final int instanceNumber,
                  final ThreadPool executor,
                  final Callback listener,
                  final HandleFactory handleFactory,
                  final boolean embedded) throws Exception {
        this.engine = Objects.requireNonNull(engine);
        Objects.requireNonNull(loader);
        this.logging = Objects.requireNonNull(logging);
        Objects.requireNonNull(workDir);
        this.context = Objects.requireNonNull(context);
        Objects.requireNonNull(executor);
        this.bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        this.log = logging.getDefault(); // server-wide log

        this.version = loader.getBuildProperty(VERSION_BUILD_PROPERTY);
        assert version != null;
        this.uvVersion = loader.getBuildProperty(LIBUV_VERSION_BUILD_PROPERTY);
        assert uvVersion != null;
        final String uv = LibUV.version();
        if (!uvVersion.equals(uv)) {
            throw new LinkageError(String.format("libuv version mismatch: expected '%s', found '%s'",
                    uvVersion,
                    uv));
        }

        this.eventLoop = new EventLoop(version, uvVersion, logging, workDir, instanceNumber, executor, handleFactory);
        this.holder = new SecureHolder(eventLoop, loader, (Invocable) engine);
        this.listener = listener;

        if (embedded) {
            // we are running embedded, keep running until explicitly closed
            // use an AsyncHandle since the close would be called from another thread
            final HandleFactory factory = eventLoop.handleFactory();
            final AsyncHandle keepAlive = factory.newAsyncHandle();
            keepAlive.setAsyncCallback(new AsyncCallback() {
                @Override
                public void onSend(int status) throws Exception {
                    keepAlive.close();
                    eventLoop.interrupt();
                }
            });
            this.keepAlive = keepAlive;
        } else {
            this.keepAlive = null;
        }
    }

    public void run(final String... args) throws Throwable {
        // No Server instance can be accessed from user scripts.
        // Although this public method is not accessible, do a permission check.
        checkPermission();
        bindings.put(SECURE_HOLDER, holder);
        try {
            LibUV.disableStdioInheritance();

            processAllArguments(args);

            if (holder.getForceRepl()) {
                runREPL();
            } else {
                if (holder.getEvalString() != null) {
                    runEval();
                } else {
                    runUserScripts();
                }
            }
        } catch (final Exception ex) {
            if (!eventLoop.handleCallbackException(ex)) {
                throw new ServerException(ex);
            }
        } finally {
            eventLoop.stop();
            logging.shutdown();
            emit("stopped");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (keepAlive != null) {
                keepAlive.send();
            }
        }
    }

    private void runSystemScript(final SystemScriptRunner... scripts) throws FileNotFoundException, ScriptException {
        if (!eventLoop.stopped()) {
            for (final SystemScriptRunner scriptRunner : scripts) {
                log.log("loading system script " + scriptRunner.script);
                scriptRunner.run(context);
            }
        }
    }

    private void runUserScripts() throws Throwable {
        assert userFile != null;

        final String[] userFiles = {userFile};
        runEventLoop(avatarArgs.toArray(new String[avatarArgs.size()]),
                     userArgs.toArray(new String[userArgs.size()]),
                     userFiles);
    }

    private void runREPL() throws Throwable {
        runEventLoop(EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    private boolean isEvalArg(String arg) {
        return ("--eval".equals(arg) || "-e".equals(arg) ||
                "--print".equals(arg) || "-pe".equals(arg) || "-p".equals(arg));
    }

    private void runEval() throws Throwable {
        runEventLoop(
                avatarArgs.toArray(new String[avatarArgs.size()]),
                userArgs.toArray(new String[userArgs.size()]),
                EMPTY_ARRAY);
    }

    private void runEventLoop(final String[] avatarArgs, final String[] userArgs, final String[] userFiles) throws Throwable {
        Throwable rootCause = null;
        holder.setArgs(avatarArgs, userArgs, userFiles);

        try {
            runSystemScript(SYSTEM_INIT_SCRIPTS);
        } catch (Throwable ex) {
            if (!eventLoop.handleCallbackException(ex)) {
                rootCause = ex;
                throw ex;
            }
        } finally {
            if (rootCause != null) {
                try {
                    // emit the process.exit event
                    runSystemScript(SYSTEM_FINALIZATION_SCRIPTS);
                } catch (Throwable ex) {
                    if (!eventLoop.handleCallbackException(ex)) {
                        rootCause.addSuppressed(ex);
                        throw rootCause;
                    }
                }
            }
        }

        emit("started");

        // ...then run the main event loop. If an exception has been handled
        // the process can continue. For example some timer events can be fired.
        try {
            eventLoop.run();
        } catch (Throwable ex) {
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
            } catch (Throwable ex) {
                if (rootCause != null) {
                    rootCause.addSuppressed(ex);
                    throw rootCause;
                }
                throw ex;
            }
        }
    }

    private void emit(final String eventName, final String... args) throws Exception {
        if (listener != null) {
            listener.call(eventName, args);
        }
    }

    private void processAllArguments(String... args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (isEvalArg(arg)) {
                    final boolean isEval = arg.indexOf('e') != -1;
                    final boolean isPrint = arg.indexOf('p') != -1;

                    // argument to -p and --print is optional
                    if (isEval && i + 1 >= args.length) {
                        System.err.println("Error: " + arg + " requires an argument\n");
                        System.exit(0);
                    }

                    holder.setPrintEval(holder.getPrintEval() || isPrint);

                    // --eval, -e and -pe always require an argument
                    if (isEval) {
                        holder.setEvalString(args[++i]);
                        continue;
                    }

                    // next arg is the expression to evaluate unless it starts with:
                    //  - a dash, then it's another switch
                    //  - "\\-", then it's an escaped expression, drop the backslash
                    if (i + 1 >= args.length) {
                        continue;
                    }
                    if (args[i + 1].charAt(0) == '-') {
                        continue;
                    }
                    final String evalString = args[++i];
                    holder.setEvalString(evalString);
                    if (evalString.startsWith("\\-")) {
                        holder.setEvalString(evalString.substring(1));
                    }
                } else {
                    if (userFile == null && holder.getEvalString() == null) {
                        if (args[i].startsWith("-")) {
                            avatarArgs.add(args[i]);
                        } else {
                            userFile = args[i];
                        }
                    } else {
                        userArgs.add(args[i]);
                    }
                }
            }
            if (userFile != null) {
                final Path p = Paths.get(userFile);
                // prefix with "./" if not absolute
                userFile = p.isAbsolute() ? p.toString() : Paths.get(".", p.toString()).toString();
            }

            log.log("avatar args " + avatarArgs);
            log.log("user file " + userFile);
            log.log("user args " + userArgs);
            processArgs(avatarArgs);
        } else {
            holder.setForceRepl(true);
        }
    }

    private void processArgs(final List<String> args) {
        boolean dumpHelp = false;
        boolean dumpVersion = false;
        boolean dumpUVVersion = false;
        String unknownArg = null;
        for (int i=0; i < args.size(); i++) {
            final String arg = args.get(i);
            if ("-h".equals(arg) || "--help".equals(arg)) {
                dumpHelp = true;
            } else if ("-v".equals(arg) || "--version".equals(arg)) {
                dumpVersion = true;
                break;
            } else if ("-uv".equals(arg) || "--uv-version".equals(arg)) {
                dumpUVVersion = true;
                break;
            } else if ("--no-deprecation".equals(arg)) {
                holder.setNoDeprecation(true);
            } else if ("--trace-deprecation".equals(arg)) {
                holder.setTraceDeprecation(true);
            } else if ("--throw-deprecation".equals(arg)) {
                holder.setThrowDeprecation(true);
            } else if ("-i".equals(arg) || "--interactive".equals(arg)) {
                holder.setForceRepl(true);
            } else {
                unknownArg = arg;
                holder.setForceRepl(true);
            }
        }

        if (dumpVersion) {
            System.out.println("v" + version);
            System.exit(0);
        }
        if (dumpUVVersion) {
            System.out.println("v" + uvVersion);
            System.exit(0);
        }
        if (dumpHelp) {
            System.out.println(HELP);
            System.exit(0);
        }
        if (unknownArg != null) {
            System.err.println("Error: unrecognized flag " + unknownArg + "\n" +
                               "Try --help for options");
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
        private String evalString;
        private boolean throwDeprecation;
        private boolean traceDeprecation;
        private boolean noDeprecation;
        private boolean forceRepl = false;
        private boolean printEval = false;
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

        private void setNoDeprecation(boolean noDeprecation) {
            this.noDeprecation = noDeprecation;
        }

        private void setForceRepl(boolean forceRepl) {
            this.forceRepl = forceRepl;
        }

        private void setPrintEval(boolean printEval) {
            this.printEval = printEval;
        }

        private void setEvalString(String evalString) {
            this.evalString = evalString;
        }

        public boolean getThrowDeprecation() {
            return throwDeprecation;
        }

        public boolean getNoDeprecation() {
            return noDeprecation;
        }

        public boolean getTraceDeprecation() {
            return traceDeprecation;
        }

        public boolean getForceRepl() {
            return forceRepl;
        }

        public boolean getPrintEval() {
            return printEval;
        }

        public String getEvalString() {
            return evalString;
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
