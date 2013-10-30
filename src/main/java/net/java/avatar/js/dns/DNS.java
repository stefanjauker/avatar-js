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

import net.java.avatar.js.eventloop.Event;
import net.java.avatar.js.eventloop.EventLoop;

public final class DNS {

    private final EventLoop eventLoop;

    public DNS(final EventLoop eventLoop) {
        this.eventLoop = eventLoop;
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
