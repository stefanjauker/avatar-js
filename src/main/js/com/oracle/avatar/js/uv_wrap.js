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
 * 2 along with this work; if not, write to the Free Software Foundation;
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* Copyright Joyent, Inc. and other Node contributors. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software", to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY;
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

(function(exports) {

  // TODO: set this up in native like UV_ERRNO_MAP in uv.cc

  var win = process.platform === 'win32';

  // uv-errno.h

  exports.UV_UNKNOWN    = -4094;
  exports.UV_EOF        = -4095;

  exports.UV_EADDRINFO  = -4091; // TODO: missing error message
  exports.UV_ECHARSET   = -4080;
  exports.UV_ENOTSUP    = -4049;

  exports.UV__EAI_ADDRFAMILY = -3000;
  exports.UV__EAI_AGAIN      = -3001;
  exports.UV__EAI_BADFLAGS   = -3002;
  exports.UV__EAI_CANCELED   = -3003;
  exports.UV__EAI_FAIL       = -3004;
  exports.UV__EAI_FAMILY     = -3005;
  exports.UV__EAI_MEMORY     = -3006;
  exports.UV__EAI_NODATA     = -3007;
  exports.UV__EAI_NONAME     = -3008;
  exports.UV__EAI_OVERFLOW   = -3009;
  exports.UV__EAI_SERVICE    = -3010;
  exports.UV__EAI_SOCKTYPE   = -3011;
  exports.UV__EAI_SYSTEM     = -3012;
  exports.UV__EAI_BADHINTS   = -3013;
  exports.UV__EAI_PROTOCOL   = -3014;

  // /usr/include/asm-generic/errno-base.h

  exports.UV_EPERM		= win ? -4048 : -1;	/* Operation not permitted */
  exports.UV_ENOENT		= win ? -4058 : -2;	/* No such file or directory */
  exports.UV_ESRCH		= win ? -4040 : -3;	/* No such process */
  exports.UV_EINTR		= win ? -4072 : -4;	/* Interrupted system call */
  exports.UV_EIO		= win ? -4070 : -5;	/* I/O error */
  exports.UV_ENXIO		= win ? -4033 : -6;	/* No such device or address */
  exports.UV_E2BIG		= win ? -4093 : -7;	/* Argument list too long */
  exports.UV_ENOEXEC	= 8;	/* Exec format error */
  exports.UV_EBADF		= win ? -4083 : -9;	/* Bad file number */
  exports.UV_ECHILD		= 10;	/* No child processes */
  exports.UV_EAGAIN		= win ? -4088 : -11;	/* Try again */
  exports.UV_ENOMEM		= win ? -4057 : -12;	/* Out of memory */
  exports.UV_EACCES		= win ? -4092 : -13;	/* Permission denied */
  exports.UV_EFAULT		= win ? -4074 : -14;	/* Bad address */
  exports.UV_ENOTBLK	= 15;	/* Block device required */
  exports.UV_EBUSY		= win ? -4082 : -16;	/* Device or resource busy */
  exports.UV_EEXIST		= win ? -4075 : -17;	/* File exists */
  exports.UV_EXDEV		= win ? -4037 : -18;	/* Cross-device link */
  exports.UV_ENODEV		= win ? -4059 : -19;	/* No such device */
  exports.UV_ENOTDIR	= win ? -4052 : -20;	/* Not a directory */
  exports.UV_EISDIR		= win ? -4068 : -21;	/* Is a directory */
  exports.UV_EINVAL		= win ? -4071 : -22;	/* Invalid argument */
  exports.UV_ENFILE		= win ? -4061 : -23;	/* File table overflow */
  exports.UV_EMFILE		= win ? -4066 : -24;	/* Too many open files */
  exports.UV_ENOTTY		= 25;	/* Not a typewriter */
  exports.UV_ETXTBSY	= win ? -4038 : -26;	/* Text file busy */
  exports.UV_EFBIG		= win ? -4036 : -27;	/* File too large */
  exports.UV_ENOSPC		= win ? -4055 : -28;	/* No space left on device */
  exports.UV_ESPIPE		= win ? -4041 : -29;	/* Illegal seek */
  exports.UV_EROFS		= win ? -4043 : -30;	/* Read-only file system */
  exports.UV_EMLINK		= 31;	/* Too many links */
  exports.UV_EPIPE		= win ? -4047 : -32;	/* Broken pipe */
  exports.UV_EDOM		= 33;	/* Math argument out of domain of func */
  exports.UV_ERANGE		= win ? -4034 : -34;	/* Math result not representable */

  // /usr/include/asm-generic/errno.h

  exports.UV_EDEADLK		= 35;	/* Resource deadlock would occur */
  exports.UV_ENAMETOOLONG	= win ? -4064 : -36;	/* File name too long */
  exports.UV_ENOLCK		    = 37;	/* No record locks available */
  exports.UV_ENOSYS		    = win ? -4054 : -38;	/* Function not implemented */
  exports.UV_ENOTEMPTY	    = win ? -4051 : -39;	/* Directory not empty */
  exports.UV_ELOOP		    = win ? -4067 : -40;	/* Too many symbolic links encountered */
  exports.UV_EWOULDBLOCK	= exports.UV_EAGAIN;	/* Operation would block */
  exports.UV_ENOMSG		    = 42;	/* No message of desired type */
  exports.UV_EIDRM		    = 43;	/* Identifier removed */
  exports.UV_ECHRNG		    = 44;	/* Channel number out of range */
  exports.UV_EL2NSYNC	    = 45;	/* Level 2 not synchronized */
  exports.UV_EL3HLT		    = 46;	/* Level 3 halted */
  exports.UV_EL3RST		    = 47;	/* Level 3 reset */
  exports.UV_ELNRNG		    = 48;	/* Link number out of range */
  exports.UV_EUNATCH		= 49;	/* Protocol driver not attached */
  exports.UV_ENOCSI		    = 50;	/* No CSI structure available */
  exports.UV_EL2HLT		    = 51;	/* Level 2 halted */
  exports.UV_EBADE		    = 52;	/* Invalid exchange */
  exports.UV_EBADR		    = 53;	/* Invalid request descriptor */
  exports.UV_EXFULL		    = 54;	/* Exchange full */
  exports.UV_ENOANO		    = 55;	/* No anode */
  exports.UV_EBADRQC		= 56;	/* Invalid request code */
  exports.UV_EBADSLT		= 57;	/* Invalid slot */

  exports.UV_EDEADLOCK	    = exports.UV_EDEADLK;

  exports.UV_EBFONT		    = 59;	/* Bad font file format */
  exports.UV_ENOSTR		    = 60;	/* Device not a stream */
  exports.UV_ENODATA		= 61;	/* No data available */
  exports.UV_ETIME		    = 62;	/* Timer expired */
  exports.UV_ENOSR		    = 63;	/* Out of streams resources */
  exports.UV_ENONET		    = win ? -4056 : -64;	/* Machine is not on the network */
  exports.UV_ENOPKG		    = 65;	/* Package not installed */
  exports.UV_EREMOTE		= 66;	/* Object is remote */
  exports.UV_ENOLINK		= 67;	/* Link has been severed */
  exports.UV_EADV		    = 68;	/* Advertise error */
  exports.UV_ESRMNT		    = 69;	/* Srmount error */
  exports.UV_ECOMM		    = 70;	/* Communication error on send */
  exports.UV_EPROTO		    = win ? -4046 : -71;	/* Protocol error */
  exports.UV_EMULTIHOP	    = 72;	/* Multihop attempted */
  exports.UV_EDOTDOT		= 73;	/* RFS specific error */
  exports.UV_EBADMSG		= 74;	/* Not a data message */
  exports.UV_EOVERFLOW	    = 75;	/* Value too large for defined data type */
  exports.UV_ENOTUNIQ	    = 76;	/* Name not unique on network */
  exports.UV_EBADFD		    = 77;	/* File descriptor in bad state */
  exports.UV_EREMCHG		= 78;	/* Remote address changed */
  exports.UV_ELIBACC		= 79;	/* Can not access a needed shared library */
  exports.UV_ELIBBAD		= 80;	/* Accessing a corrupted shared library */
  exports.UV_ELIBSCN		= 81;	/* .lib section in a.out corrupted */
  exports.UV_ELIBMAX		= 82;	/* Attempting to link in too many shared libraries */
  exports.UV_ELIBEXEC	    = 83;	/* Cannot exec a shared library directly */
  exports.UV_EILSEQ		    = 84;	/* Illegal byte sequence */
  exports.UV_ERESTART	    = 85;	/* Interrupted system call should be restarted */
  exports.UV_ESTRPIPE	    = 86;	/* Streams pipe error */
  exports.UV_EUSERS		    = 87;	/* Too many users */
  exports.UV_ENOTSOCK	    = win ? -4050 : -88;	/* Socket operation on non-socket */
  exports.UV_EDESTADDRREQ	= win ? -4076 : -89;	/* Destination address required */
  exports.UV_EMSGSIZE	    = win ? -4065 : -90;	/* Message too long */
  exports.UV_EPROTOTYPE	    = win ? -4044 : -91;	/* Protocol wrong type for socket */
  exports.UV_ENOPROTOOPT	= win ? -4035 : -92;	/* Protocol not available */
  exports.UV_EPROTONOSUPPORT	= win ? -4045 : -93;	/* Protocol not supported */
  exports.UV_ESOCKTNOSUPPORT	= 94;	/* Socket type not supported */
  exports.UV_EOPNOTSUPP	    = 95;	/* Operation not supported on transport endpoint */
  exports.UV_EPFNOSUPPORT	= 96;	/* Protocol family not supported */
  exports.UV_EAFNOSUPPORT	= win ? -4089 : -97;	/* Address family not supported by protocol */
  exports.UV_EADDRINUSE	    = win ? -4091 : -98;	/* Address already in use */
  exports.UV_EADDRNOTAVAIL	= win ? -4090 : -99;	/* Cannot assign requested address */
  exports.UV_ENETDOWN	    = win ? -4063 : -100;	/* Network is down */
  exports.UV_ENETUNREACH	= win ? -4063 : -101;	/* Network is unreachable */
  exports.UV_ENETRESET	    = 102;	/* Network dropped connection because of reset */
  exports.UV_ECONNABORTED	= win ? -4079 : -103;	/* Software caused connection abort */
  exports.UV_ECONNRESET	    = win ? -4077 : -104;	/* Connection reset by peer */
  exports.UV_ENOBUFS		= win ? -4060 : -105;	/* No buffer space available */
  exports.UV_EISCONN		= win ? -4069 : -106;	/* Transport endpoint is already connected */
  exports.UV_ENOTCONN	    = win ? -4053 : -107;	/* Transport endpoint is not connected */
  exports.UV_ESHUTDOWN	    = win ? -4042 : -108;	/* Cannot send after transport endpoint shutdown */
  exports.UV_ETOOMANYREFS	= 109;	/* Too many references: cannot splice */
  exports.UV_ETIMEDOUT	    = win ? -4039 : -110;	/* Connection timed out */
  exports.UV_ECONNREFUSED	= win ? -4078 : -111;	/* Connection refused */
  exports.UV_EHOSTDOWN	    = 112;	/* Host is down */
  exports.UV_EHOSTUNREACH	= win ? -4073 : -113;	/* No route to host */
  exports.UV_EALREADY	    = win ? -4084 : -114;	/* Operation already in progress */
  exports.UV_EINPROGRESS	= 115;	/* Operation now in progress */
  exports.UV_ESTALE		    = 116;	/* Stale NFS file handle */
  exports.UV_EUCLEAN		= 117;	/* Structure needs cleaning */
  exports.UV_ENOTNAM		= 118;	/* Not a XENIX named type file */
  exports.UV_ENAVAIL		= 119;	/* No XENIX semaphores available */
  exports.UV_EISNAM		    = 120;	/* Is a named type file */
  exports.UV_EREMOTEIO	    = 121;	/* Remote I/O error */
  exports.UV_EDQUOT		    = 122;	/* Quota exceeded */

  exports.UV_ENOMEDIUM	    = 123;	/* No medium found */
  exports.UV_EMEDIUMTYPE	= 124;	/* Wrong medium type */
  exports.UV_ECANCELED	    = win ? -4081 : -125;	/* Operation Canceled */
  exports.UV_ENOKEY		    = 126;	/* Required key not available */
  exports.UV_EKEYEXPIRED	= 127;	/* Key has expired */
  exports.UV_EKEYREVOKED	= 128;	/* Key has been revoked */
  exports.UV_EKEYREJECTED	= 129;	/* Key was rejected by service */

  /* for robust mutexes */
  exports.UV_EOWNERDEAD	    = 130;	/* Owner died */
  exports.UV_ENOTRECOVERABLE	= 131;	/* State not recoverable */

  exports.UV_ERFKILL		= 132;	/* Operation not possible due to RF-kill */
  exports.UV_EHWPOISON	    = 133;	/* Memory page has hardware error */

  var errnames = {};

  // from uv.h v0.11.22, keep in sync
  errnames[exports.UV_E2BIG] = "argument list too long";
  errnames[exports.UV_EACCES] = "permission denied";
  errnames[exports.UV_EADDRINUSE] = "address already in use";
  errnames[exports.UV_EADDRNOTAVAIL] = "address not available";
  errnames[exports.UV_EAFNOSUPPORT] = "address family not supported";
  errnames[exports.UV_EAGAIN] = "resource temporarily unavailable";
  errnames[exports.UV_EAI_ADDRFAMILY] = "address family not supported";
  errnames[exports.UV_EAI_AGAIN] = "temporary failure";
  errnames[exports.UV_EAI_BADFLAGS] = "bad ai_flags value";
  errnames[exports.UV_EAI_BADHINTS] = "invalid value for hints";
  errnames[exports.UV_EAI_CANCELED] = "request canceled";
  errnames[exports.UV_EAI_FAIL] = "permanent failure";
  errnames[exports.UV_EAI_FAMILY] = "ai_family not supported";
  errnames[exports.UV_EAI_MEMORY] = "out of memory";
  errnames[exports.UV_EAI_NODATA] = "no address";
  errnames[exports.UV_EAI_NONAME] = "unknown node or service";
  errnames[exports.UV_EAI_OVERFLOW] = "argument buffer overflow";
  errnames[exports.UV_EAI_PROTOCOL] = "resolved protocol is unknown";
  errnames[exports.UV_EAI_SERVICE] = "service not available for socket type";
  errnames[exports.UV_EAI_SOCKTYPE] = "socket type not supported";
  errnames[exports.UV_EAI_SYSTEM] = "system error";
  errnames[exports.UV_EALREADY] = "connection already in progress";
  errnames[exports.UV_EBADF] = "bad file descriptor";
  errnames[exports.UV_EBUSY] = "resource busy or locked";
  errnames[exports.UV_ECANCELED] = "operation canceled";
  errnames[exports.UV_ECHARSET] = "invalid Unicode character";
  errnames[exports.UV_ECONNABORTED] = "software caused connection abort";
  errnames[exports.UV_ECONNREFUSED] = "connection refused";
  errnames[exports.UV_ECONNRESET] = "connection reset by peer";
  errnames[exports.UV_EDESTADDRREQ] = "destination address required";
  errnames[exports.UV_EEXIST] = "file already exists";
  errnames[exports.UV_EFAULT] = "bad address in system call argument";
  errnames[exports.UV_EFBIG] = "file too large";
  errnames[exports.UV_EHOSTUNREACH] = "host is unreachable";
  errnames[exports.UV_EINTR] = "interrupted system call";
  errnames[exports.UV_EINVAL] = "invalid argument";
  errnames[exports.UV_EIO] = "i/o error";
  errnames[exports.UV_EISCONN] = "socket is already connected";
  errnames[exports.UV_EISDIR] = "illegal operation on a directory";
  errnames[exports.UV_ELOOP] = "too many symbolic links encountered";
  errnames[exports.UV_EMFILE] = "too many open files";
  errnames[exports.UV_EMSGSIZE] = "message too long";
  errnames[exports.UV_ENAMETOOLONG] = "name too long";
  errnames[exports.UV_ENETDOWN] = "network is down";
  errnames[exports.UV_ENETUNREACH] = "network is unreachable";
  errnames[exports.UV_ENFILE] = "file table overflow";
  errnames[exports.UV_ENOBUFS] = "no buffer space available";
  errnames[exports.UV_ENODEV] = "no such device";
  errnames[exports.UV_ENOENT] = "no such file or directory";
  errnames[exports.UV_ENOMEM] = "not enough memory";
  errnames[exports.UV_ENONET] = "machine is not on the network";
  errnames[exports.UV_ENOPROTOOPT] = "protocol not available";
  errnames[exports.UV_ENOSPC] = "no space left on device";
  errnames[exports.UV_ENOSYS] = "function not implemented";
  errnames[exports.UV_ENOTCONN] = "socket is not connected";
  errnames[exports.UV_ENOTDIR] = "not a directory";
  errnames[exports.UV_ENOTEMPTY] = "directory not empty";
  errnames[exports.UV_ENOTSOCK] = "socket operation on non-socket";
  errnames[exports.UV_ENOTSUP] = "operation not supported on socket";
  errnames[exports.UV_EPERM] = "operation not permitted";
  errnames[exports.UV_EPIPE] = "broken pipe";
  errnames[exports.UV_EPROTO] = "protocol error";
  errnames[exports.UV_EPROTONOSUPPORT] = "protocol not supported";
  errnames[exports.UV_EPROTOTYPE] = "protocol wrong type for socket";
  errnames[exports.UV_ERANGE] = "result too large";
  errnames[exports.UV_EROFS] = "read-only file system";
  errnames[exports.UV_ESHUTDOWN] = "cannot send after transport endpoint shutdown";
  errnames[exports.UV_ESPIPE] = "invalid seek";
  errnames[exports.UV_ESRCH] = "no such process";
  errnames[exports.UV_ETIMEDOUT] = "connection timed out";
  errnames[exports.UV_ETXTBSY] = "text file is busy";
  errnames[exports.UV_EXDEV] = "cross-device link not permitted";
  errnames[exports.UV_UNKNOWN] = "unknown error";
  errnames[exports.UV_EOF] = "end of file"
  errnames[exports.UV_ENXIO] = "no such device or address"

  exports.errname = function(err) {
    return errnames[err];
  }

});
