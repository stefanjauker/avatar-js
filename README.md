Build Instructions
==================

1. Setup build environment. Install a git client, jdk8, ant, python, C++ compiler and linker

   1. Install the latest [jdk8](http://jdk8.java.net/download.html)
   2. Ensure [ant](http://ant.apache.org/) is installed and `ant -version` is _1.8_+
   3. Ensure [python](http://python.org/) is installed and `python --version` is _2.7.5_+ <br/>
      _tip_: __python3__ does __not__ work
   4. Ensure ant, jdk8, python and appropriate platform-specific _C++_ compiler and linker are in the `PATH`
   5. If needed, install a [git](http://git-scm.com/) client for your OS. Windows users see below
   6. Extra steps for Windows users
      1. Install a unix emulation like [cygwin](http://cygwin.com/) or
         [mingw/msys](http://mingw.org/). If using [Git for Windows](http://msysgit.github.io/),
         a version of msys is already [included](https://github.com/msysgit/msysgit/wiki/Frequently-Asked-Questions).
         This unix emulation provides the gnu diff and patch utilities.
      2. Install [Microsoft Visual Studio](http://www.visualstudio.com/en-us).
         Any recent version should be OK (2010, 2012, 2013 have been known to work).
         Express versions will do.
      3. Ensure that link.exe is the one from Visual Studio and not from cygwin or mingw/msys.
      4. If a unix user, start `bash --login` from within a Visual Studio Command Prompt to
         ensure that the Visual Studio environment is inherited.

2. Download [node.js](http://nodejs.org/) source

   1. `git clone https://github.com/joyent/node.git source`
   2. `cd source`
   3. `git checkout v0.10.25` <br/>
      _tip_: run `git pull` to refresh the repo if you get an error saying that this version is unknown
   4. `cd ..`

3. Clone dependencies. Substitute `<id>` with your [java.net](https://home.java.net/)
   id in the URLs below. Use the _read-only_ URLs below if you do not have a
   [java.net](https://home.java.net/) id.
   It is strongly recommended that all dependencies be cloned at the same level as
   node.js as in step 2.1 above

   1. `git clone git://java.net/avatar-js~libuv-java libuv-java` _(read-only)_ <br/>
      `git clone ssh://<id>@git.java.net/avatar-js~libuv-java libuv-java`

   2. `git clone git://java.net/avatar-js~http-parser-java http-parser-java` _(read-only)_ <br/>
      `git clone ssh://<id>@git.java.net/avatar-js~http-parser-java http-parser-java`

   3. `git clone git://java.net/avatar-js~src avatar-js` _(read-only)_ <br/>
      `git clone ssh://<id>@git.java.net/avatar-js~src avatar-js`

   4. Download [test-ng](http://testng.org/doc/download.html).
      Unzip the downloaded bundle and place testng jar somewhere in your workspace

4. Edit `~/.avatar-js.properties` to set various locations.
   Ensure that there are __no__ double slashes and __no__ trailing slash, for example

   `source.home = /ws/source` (from 2.1) <br/>
   `libuv.home = /ws/libuv-java` (from 3.1) <br/>
   `http-parser.home = /ws/http-parser-java` (from 3.2) <br/>
   `avatar-js.home = /ws/avatar-js` (from 3.3) <br/>
   `testng.jar = /ws/lib/testng.jar` (from 3.4) <br/>

5. Setup your environment so that native libraries can be found

   `export LD_LIBRARY_PATH=$PWD/dist:$LD_LIBRARY_PATH` _(linux)_ <br/>
   `export DYLD_LIBRARY_PATH=$PWD/dist:$DYLD_LIBRARY_PATH` _(mac)_ <br/>
   `export PATH=%CWD%/dist;%PATH%` _(windows)_ <br/>

6. Configure

   `cd avatar-js` <br/>
   `ant setup`

7. Build

   `ant clean-all` <br/>
   `ant jar-all` <br/>
   _tip_: `ant -Dbuild.type=Release jar-all` builds the Release configuration

8. Test

   `java -jar dist/avatar-js.jar src/test/js/test-runner.js test/simple` (runs the unit tests) <br/>
   `java -jar dist/avatar-js.jar src/test/js/test-runner.js test/crypto` (runs the crypto tests) <br/>
   `java -jar dist/avatar-js.jar src/test/js/test-runner.js -timeout 180 test/pummel` (runs the pummel tests) <br/>

   _tip_: Some crypto tests require
      [Java Cryptography Extension Unlimited Jurisdiction Policy Files](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
      Follow the instructions in the download. Files for Java7 work in Java8.

   _tip_: Might need to set `LANG=en_US.UTF-8`

   _tip_: `test-runner.js` supports many options, run it with `-help` or look at its source for more information

9. Running applications. For example

   `java -jar dist/avatar-js.jar app.js` <br/>
   or <br/>
   `java -Djava.library.path=dist -jar dist/avatar-js.jar app.js` <br/>
   or <br/>
   `java -Xmx4g -Djava.library.path=dist -jar dist/avatar-js.jar app.js` <br/>

Release Notes
=============

+ Compatible with Node.js v0.10.25

+ Platforms tested. 32-bit variants have not been tested _at all_
  + Ubuntu Linux x64
  + Oracle Enterprise Linux x64
  + Mac OS X Mountain Lion
  + Windows 7 & 8 x64

+ Missing Features
  + the `debugger` module is not supported

+ Known failures
  + some tests fail on windows. These are listed in `win32-test-exclusions.txt` with associated issue

+ Globals. Some additional globals are exposed
  + [javax.script.ScriptContext](http://download.java.net/jdk8/docs/api/javax/script/ScriptContext.html)
  + `__avatar` _(non-iterable)_, a container for some _avatar-specific_ globals _for internal use_

+ Limitations of the `vm` module. Objects loaded in a new context and accessed
  from the current context have some strong limitations -
  + Only a subset of JavaScript API is available on these objects
  + equality (==, ===) will not work as expected

+ Limitations on `__proto__`
   + `__proto__` cannot be set to `undefined`. Affects the `ejs` module used by `express`

+ No `const` keyword
   + the `const` keyword is not supported, replace with `var`. Affects the `mongodb` and `grunt` modules

+ No signal handlers installed by default
   + the JVM installs some of its own and we do not want to cause conflicts
   + an app can install signal handlers as needed using
      `process.signals.start('SIGUSR1');`
   or
      `process.signals.start(43);`

