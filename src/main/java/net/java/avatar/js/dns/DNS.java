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

package net.java.avatar.js.dns;

import net.java.libuv.Callback;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.NoSuchAttributeException;

import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

public final class DNS {

    private final EventLoop eventLoop;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private DirContext provider;

    public DNS(final EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    private String[] resolve(final String domain, final String record) throws NamingException {
        // initialize lazily on the very first request
        if (!initialized.get()) {
            synchronized (initialized) {
                @SuppressWarnings("UseOfObsoleteCollectionType")
                final Hashtable<String,String> env = new Hashtable<String, String>();
                env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
                provider = new InitialDirContext(env);
                initialized.set(true);
            }
        }
        final Attributes query = provider.getAttributes(domain, new String[] { record });
        final Attribute records = query.get(record);
        if (records == null) {
            throw new NoSuchAttributeException("No attributes found");
        }
        @SuppressWarnings("rawtypes")
        final NamingEnumeration recordData = records.getAll();
        final int size = records.size();
        final String[] data = new String[size];
        for (int i = 0; i < size; i++) {
            data[i] = recordData.next().toString();
        }
        return data;
    }

    public void query(final String address,
                      final String type,
                      final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final String[] addresses = resolve(address, type);
                    for (int i = 0; i < addresses.length; i++) {
                        if (addresses[i].startsWith("\"") && addresses[i].endsWith("\"")) {
                            addresses[i] = addresses[i].substring(1, addresses[i].length() - 2);
                        }
                    }
                    eventLoop.post(new Event("dns.resolve.query." + type, callback, null, addresses));
                } catch (final NamingException e ) {
                    eventLoop.post(new Event("dns.resolve.query." + type + ".error", callback, e, null));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public void getHostByAddress(final String address,
                                 final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final InetAddress[] hostAddresses = InetAddress.getAllByName(address);
                    final String[] hostNames = new String[hostAddresses.length];
                    for (int i = 0; i < hostAddresses.length; i++) {
                        hostNames[i] = hostAddresses[i].getHostName();
                    }
                    eventLoop.post(new Event("dns.host", callback, null, hostNames));
                } catch (final UnknownHostException e) {
                    eventLoop.post(new Event("dns.host.error", callback, e, null));
                } finally {
                    handle.close();
                }
            }
        });
    }

    public void getAddressByHost(final String hostname,
                                 final Callback callback) {
        final EventLoop.Handle handle = eventLoop.grab();
        eventLoop.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final InetAddress[] hostAddresses = InetAddress.getAllByName(hostname);
                    final String[] addresses = new String[hostAddresses.length];
                    for (int i = 0; i < hostAddresses.length; i++) {
                        addresses[i] = hostAddresses[i].getHostAddress();
                    }
                    eventLoop.post(new Event("dns.address", callback, null, addresses));
                } catch (final UnknownHostException e) {
                    eventLoop.post(new Event("dns.address.error", callback, e, null));
                } finally {
                    handle.close();
                }
            }
        });
    }

}
