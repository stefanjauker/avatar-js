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

#include <string.h>

#ifdef __POSIX__
# include <sys/utsname.h>
#else
#include <Windows.h>
#endif

#include "net_java_avatar_js_os_OS.h"

/*
 * Class:     net_java_avatar_js_os_OS
 * Method:    _getType
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_net_java_avatar_js_os_OS__1getType
  (JNIEnv *env, jclass cls) {

#ifdef __POSIX__
  char type[256];
  struct utsname info;

  uname(&info);
  strncpy(type, info.sysname, strlen(info.sysname));
  type[strlen(info.sysname)] = 0;

  return env->NewStringUTF(type);
#else // __MINGW32__
  return env->NewStringUTF("Windows_NT");
#endif
}

/*
 * Class:     net_java_avatar_js_os_OS
 * Method:    _getRelease
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_net_java_avatar_js_os_OS__1getRelease
  (JNIEnv *env, jclass cls) {

  char release[256];

#ifdef __POSIX__
  struct utsname info;

  uname(&info);
  strncpy(release, info.release, strlen(info.release));
  release[strlen(info.release)] = 0;
#else // __MINGW32__
  OSVERSIONINFO info;
  info.dwOSVersionInfoSize = sizeof(info);

  if (GetVersionEx(&info) == 0) {
    return NULL;
  }
  sprintf(release, "%d.%d.%d", static_cast<int>(info.dwMajorVersion),
      static_cast<int>(info.dwMinorVersion), static_cast<int>(info.dwBuildNumber));
#endif

  return env->NewStringUTF(release);
}

/*
 * Class:     net_java_avatar_js_os_OS
 * Method:    _getEndianness
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_net_java_avatar_js_os_OS__1getEndianness
  (JNIEnv *env, jclass cls) {

  int i = 1;
  bool big = (*(char *)&i) == 0;
  return env->NewStringUTF(big ? "BE" : "LE");
}
