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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * An extensible module loader.
 */
public abstract class Loader {

    static {
        // XXX jfdenise, this could be revisited with other way to find out debug mode
        List<String> debug_keys = new ArrayList<>();
        debug_keys.add("-Xdebug");
        debug_keys.add("-agentlib:jdwp");

        boolean debug = false;
        try {
            Class<?> clazz = Class.forName("java.lang.management.ManagementFactory");
            Class<?> mxbeanClazz = Class.forName("java.lang.management.RuntimeMXBean");
            Object mxbean = clazz.getMethod("getRuntimeMXBean").invoke(null);
            @SuppressWarnings("unchecked")
            List<String> arguments = (List<String>) mxbeanClazz.getDeclaredMethod("getInputArguments").invoke(mxbean);
            for (String a : arguments) {
                // does it starts with
                for (String k : debug_keys) {
                    if (a.startsWith(k)) {
                        debug = true;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            // XXX OK compact1 or something
        }

        /*
         * If we are in debugging, the runtime source file is exposed to the user.
         * So it must be cleanly formatted and a \n is expected. If a stack trace is displayed,
         * the line numbers must be in sync with the file exposed to the user (The Runtime Source).
         * In this case the function signature is postfixed by a \n for proper formatting.
         * If we are NOT in debugging, the original source file is exposed to the user.
         * No function wrapper visible. If a stack trace is displayed,
         * the line numbers must be in sync with the file exposed to the user (The original Source).
         * In this case the function signature is not postfixed by a \n.
         */
        final String p = "(function (exports, require, module, __filename, __dirname) { ";
        if (debug) { // Runtime source is exposed.
            PREFIX = p + "\n";
        } else { // Not exposed, so hide wrapping.
            PREFIX = p;
        }
    }

    public static final String PREFIX;
    public static final String SUFFIX = "\n});";

    public static String wrap(final String content) {
        return PREFIX + content + SUFFIX;
    }

    public URL wrapURL(final URL url) throws MalformedURLException {
        return new URL(null, url.toExternalForm(), newHandler());
    }

    public URL wrapURL(final String reference) throws MalformedURLException {
        // Is it an URL?
        URL url;
        try {
            url = new URL(reference);
        } catch (final Exception ex) {
            // No, make it a file URL
            url = new File(reference).toURI().toURL();
        }
        return wrapURL(url);
    }

    protected URLStreamHandler newHandler() {
        return new WrappingStreamHandler();
    }

    public static final String SCRIPT_EXTENSION = ".js";
    public static final String UTF_8 = "UTF-8";

    /**
     * Tests whether the specified module is available from
     * this loader.
     * @param id The id.
     * @return {@code true} if found, or {@code false} if not.
     */
    public abstract boolean exists(final String id);

    /**
     * Returns the value of a compiled-in property.
     * @param key the key whose value is desired
     * @return the property value, null if not found
     */
    public abstract String getBuildProperty(final String key);

    public URL getURL(final String id) throws Exception {
        return findURL(id);
    }

    /**
     * Returns a {@code URL} for the specified id.
     * @param id The id.
     * @return The {@code URL} or {@code null} if not found.
     * @throws MalformedURLException if an error occurs.
     */
    protected abstract URL findURL(final String id) throws MalformedURLException;

    /**
     * A {@link Loader} for the built-in modules.
     */
    public static class Core extends Loader {

        private static final String PROPERTIES_PATH = "/build.properties";
        private static final String MODULES_KEY = "avatar-js.builtin.modules";
        private static final String MODULES_DIR = "lib/";
        private static final Properties BUILD_PROPERTIES = new Properties();

        private final Set<String> coreModules;
        private final ClassLoader classLoader;

        /**
         * Constructor.
         */
        public Core() {
            final InputStream is = Core.class.getResourceAsStream(PROPERTIES_PATH);
            assert null != is;
            try {
                BUILD_PROPERTIES.load(is);
            } catch (final IOException ex) {
                if (Server.assertions()) {
                    ex.printStackTrace();
                }
            }

            coreModules = getCoreModuleNames();
            assert !coreModules.isEmpty();
            classLoader = getClass().getClassLoader();
        }

        /**
         * Tests whether the specified module is available from
         * this loader.
         * @param id The id.
         * @return {@code true} if found, or {@code false} if not.
         */
        @Override
        public boolean exists(final String id) {
            return coreModules.contains(id);
        }

        @Override
        public String getBuildProperty(final String key) {
            return BUILD_PROPERTIES.getProperty(key);
        }

        protected String pathFor(final String id) {
            return MODULES_DIR + id + SCRIPT_EXTENSION;
        }

        private static Set<String> getCoreModuleNames() {
            final String modulesList = BUILD_PROPERTIES.getProperty(MODULES_KEY);
            assert modulesList != null;
            return new HashSet<>(Arrays.asList(modulesList.split("\\s")));
        }

        @Override
        protected URL findURL(final String id) throws MalformedURLException {
            if (coreModules.contains(id)) {
                return wrapURL(classLoader.getResource(pathFor(id)));
            }
            return null;
        }
    }
}
