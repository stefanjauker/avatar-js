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
#include <stdlib.h>
#include <errno.h>

#ifdef __POSIX__
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#include <unistd.h>  // getpid etc
#include <grp.h> // group stuff
#else
#include <process.h>
#include <io.h>
#define getpid _getpid
#define umask _umask
typedef int mode_t;
#endif

#include "throw.h"
#include "net_java_avatar_js_os_Process.h"

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _abort
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1abort
  (JNIEnv *env, jclass cls) {

  abort();
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _getPid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_net_java_avatar_js_os_Process__1getPid
  (JNIEnv *env, jclass cls) {

#ifdef __POSIX__
    return getpid();
#else
    return _getpid();
#endif
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _getUid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_net_java_avatar_js_os_Process__1getUid
  (JNIEnv *env, jclass cls) {

#ifdef __POSIX__
    return getuid();
#else
    return -1;
#endif
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _setUid
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1setUid
  (JNIEnv *env, jclass cls, jint uid) {

#ifdef __POSIX__
  int result;
  if ((result = setuid(uid)) != 0) {
    ThrowException(env, "setuid");
  }
#endif
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _getGid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_net_java_avatar_js_os_Process__1getGid
  (JNIEnv *env, jclass cls) {

#ifdef __POSIX__
    return getgid();
#else
    return -1;
#endif
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _setGid
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1setGid
  (JNIEnv *env, jclass cls, jint gid) {

#ifdef __POSIX__
  int result;
  if ((result = setgid(gid)) != 0) {
    ThrowException(env, "setgid");
  }
#endif
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _umask
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_net_java_avatar_js_os_Process__1umask
  (JNIEnv *env, jclass cls, jint mask) {
  return umask(static_cast<mode_t>(mask));
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _getUmask
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_net_java_avatar_js_os_Process__1getUmask
  (JNIEnv *env, jclass cls) {
  unsigned int old = umask(0);
  umask((mode_t)old);
  return old;
}

#ifdef __POSIX__

static const gid_t gid_not_found = static_cast<gid_t>(-1);

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _getGroups
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_net_java_avatar_js_os_Process__1getGroups
  (JNIEnv *env, jclass cls) {

  int ngroups = getgroups(0, NULL);

  if (ngroups == -1) {
    ThrowException(env, "getgroups");
  }

  gid_t* groups = new gid_t[ngroups];

  ngroups = getgroups(ngroups, groups);

  if (ngroups == -1) {
    delete[] groups;
    ThrowException(env, "getgroups");
  }

  jintArray array = env->NewIntArray(ngroups);
  jint *arr = env->GetIntArrayElements(array, NULL);
  bool seen_egid = false;
  gid_t egid = getegid();

  for (int i = 0; i < ngroups; i++) {
    arr[i] = (int) groups[i];
    if (groups[i] == egid) seen_egid = true;
  }

  env->ReleaseIntArrayElements(array, arr, 0);

  delete[] groups;

  if (seen_egid == false) {
    jintArray array2 = env->NewIntArray(ngroups + 1);
    env->SetIntArrayRegion(array2, 0, ngroups, arr);
    jint *arr2 = env->GetIntArrayElements(array2, NULL);
    arr2[ngroups] = egid;
    env->ReleaseIntArrayElements(array2, arr2, 0);
    delete array;
    array = array2;
  }

  return array;
}

static gid_t gid_by_name(const char* name) {
  struct group pwd;
  struct group* pp;
  char buf[8192];

  errno = 0;
  pp = NULL;

  if (getgrnam_r(name, &pwd, buf, sizeof(buf), &pp) == 0 && pp != NULL) {
    return pp->gr_gid;
  }

  return gid_not_found;
}

static char* name_by_uid(uid_t uid) {
  struct passwd pwd;
  struct passwd* pp;
  char buf[8192];
  int rc;

  errno = 0;
  pp = NULL;

  if ((rc = getpwuid_r(uid, &pwd, buf, sizeof(buf), &pp)) == 0 && pp != NULL) {
    return strdup(pp->pw_name);
  }

  if (rc == 0) {
    errno = ENOENT;
  }

  return NULL;
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _setGroups
 * Signature: ([Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1setGroups
  (JNIEnv *env, jclass cls, jobjectArray array) {

  jsize size = env->GetArrayLength(array);
  gid_t* groups = new gid_t[size];
  for (size_t i = 0; i < (size_t)size; i++) {
    jstring str = (jstring) env->GetObjectArrayElement(array, i);
    const char *content = env->GetStringUTFChars(str, 0);
    gid_t gid = gid_by_name(content);

    if (gid == gid_not_found) {
      delete[] groups;
      ThrowException(env, "getgrnam_r");
    }

    groups[i] = gid;
  }
  int rc = setgroups(size, groups);
  delete[] groups;
  if (rc == -1) {
    ThrowException(env, "setgroups");
  }
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _initGroups_S_S
 * Signature: ([Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1initGroups_1S_1S
  (JNIEnv *env, jclass cls, jstring user, jstring group) {

  const char* u = env->GetStringUTFChars(user, 0);
  const char* g = env->GetStringUTFChars(group, 0);

  gid_t extra_group = gid_by_name(g);

  if (extra_group == gid_not_found) {
    ThrowException(env, "getgrnam_r");
  }

  int rc = initgroups(u, extra_group);

  if (rc) {
    ThrowException(env, "initgroups");
  }
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _initGroups_I_I
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1initGroups_1I_1I
  (JNIEnv *env, jclass cls, jint user, jint group) {
  gid_t extra_group = group;
  const char* u = name_by_uid(user);
  if (u == NULL) {
    ThrowException(env, "getpwuid_r");
  }

  int rc = initgroups(u, extra_group);

  if (rc) {
    ThrowException(env, "initgroups");
  }
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _initGroups_I_S
 * Signature: (I[Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1initGroups_1I_1S
  (JNIEnv *env, jclass cls, jint user, jstring group) {
    const char* u = name_by_uid(user);
    if (u == NULL) {
        ThrowException(env, "getpwuid_r");
    }

    const char* g = env->GetStringUTFChars(group, 0);
    gid_t extra_group = gid_by_name(g);
    if (extra_group == gid_not_found) {
        ThrowException(env, "getgrnam_r");
    }

    int rc = initgroups(u, extra_group);

    if (rc) {
        ThrowException(env, "initgroups");
    }
}

/*
 * Class:     net_java_avatar_js_os_Process
 * Method:    _initGroups_S_I
 * Signature: ([Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_net_java_avatar_js_os_Process__1initGroups_1S_1I
  (JNIEnv *env, jclass cls, jstring user, jint group) {
    const char* u = env->GetStringUTFChars(user, 0);

    int rc = initgroups(u, group);

    if (rc) {
        ThrowException(env, "initgroups");
    }
}
#endif

