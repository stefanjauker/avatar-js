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

(function(exports) {

   var CTX_PROPERTY_NAME = "__avatar_ctx";

   exports.NodeScript = function(source, ctx, filename) {
       this._source = Buffer.isBuffer(source) ? source.toString() : source;
       if (ctx) {
           if (typeof ctx === 'string') {
               this._filename = ctx;
           } else {
               this._ctx = ctx;
               if (filename) {
                   this._filename = filename;
               }
           }
       }
   }

   /**
    * Static load of filename URL or code. This is the entry point for all
    * module require('xxx') loading.
    * Script loading is done in current (or this) context.
    */
   exports.NodeScript.runInThisContext = function(code, filename, displayError) {
       if (code == null) {
           return load(__avatar.loader.wrapURL(filename));
       } else {
           // Done outside any codebase.
           return load({script: code, name: filename});
       }
   }

   /**
    * Load of this Script filename URL or code. This is the entry point for all
    * module require('xxx') loading.
    */
   exports.NodeScript.prototype.runInThisContext = function() {
       return exports.NodeScript.runInThisContext(this._source, this._filename);
   }

   /* All the functions defined here after are dealing with foreign global context.
    *
    * createContext: Create a new global context, copying definitions of passed init parameter
    * runInContext: run a script in a new global context created with createContext.
    * runInNewContext: use an object instance of the current context as a foreign
    * global context.
    *
    */

   /**
    * Static creation of a new global context used for execution in a foreign global context.
    * A shalow copy of the init instance is set in the returned context. There is no sharing between
    * the current context and new context.
    */
   exports.NodeScript.createContext = function(initSandbox) {
       var context = {};

       // The optional argument initSandbox will be shallow-copied to seed
       // the initial contents of the global object used by the context.
       if (initSandbox) {
           var names = Object.getOwnPropertyNames(initSandbox);
           var length = names.length;
           for (var i=0; i < length; i++) {
               var name = names[i];
               // First level references to initSandbox are replaced by clone.
               // This is undocumented but tested by test-vm-create-context-circular-reference.js
               if (initSandbox[name] === initSandbox) {
                   context[name] = context;
               } else {
                   var desc = Object.getOwnPropertyDescriptor(initSandbox, name);
                   Object.defineProperty(context, name,  desc);
               }
           }
       }
       return context;
   }


   /**
    * Creation of a new global context used for execution in a foreign global context.
    * A shalow copy of the init instance is set in the returned context. There is no sharing between
    * the current context and new context.
    */
   exports.NodeScript.prototype.createContext = function(init) {
       return exports.NodeScript.createContext(init);
   }

   /**
    * A new global scope. init has been created from createContext (runInContext)
    * or is a user object (runInNewContext).
    */
   function newGlobalScope(context) {
        var init_script = new_global_prefix(context);

       // _global comes from foreign context, this is a ScriptObjectMirror
       // it has some limitations
       var _global = loadWithNewGlobal({ name: "new_scope.js",
           script: init_script +
                   "this" }, context);
       return _global; 
   }

   /**
    * This is the prefix executed in the new global context.
    * This prefix transforms user passed arguments onto global definitions.
    * When a global property is get or set, the actual arguments[0] is called.
    */
   function new_global_prefix(init) {
       var prefix = "var " + CTX_PROPERTY_NAME + " = arguments[0];\n";
       var names = Object.getOwnPropertyNames(init);
       var length = names.length;
       for (var i=0; i < length; i++) {
           var key = names[i];
           var desc = Object.getOwnPropertyDescriptor(init, key);
           if (desc.value !== undefined) {
               prefix += "this['"+key+"'] = arguments[0]['" + key +  "'];\n";
           } else {
               prefix += "Object.defineProperty(this, '" + key + "', {\n";
               prefix += "get: function() { return " + CTX_PROPERTY_NAME + "." + key + "},\n";
               prefix += "set: function(val) { " + CTX_PROPERTY_NAME + "." + key + " = val },\n";
               prefix += "});\n"
           }
       }

       return prefix;
   }

   /*
    * Static execution of script inside the passed global context.
    * The core of this function is the use of nashorn loadWithNewGlobal
    */
   exports.NodeScript.runInContext = function(code, context, filename) {
       if (!code) {
           throw new Error('no source: ' + code);
       }
       if (!context) {
           throw new TypeError('undefined context: ' + context);
       }

       code = Buffer.isBuffer(code) ? code.toString() : code;

       var name = filename ? filename : "<script>";

       // global needs to reconstructed each time evaluation is done, context could have
       // dynamically updated.
       // _global is a SciptObjectMirror, load can't be called on it.
       // only eval can be used.
       var _global = newGlobalScope(context);
       var loader = _global.eval("(function loader(code, filename) { return load({name:filename, script:code}); });");
       var res = loader(code, name);

       // Export to context
       syncContext(context, _global);

       return res;
   }

   exports.NodeScript.prototype.runInContext = function(context) {
       return exports.NodeScript.runInContext(this._source, context, this._filename);
   }

   /**
    * Feed the context and the sandbox (if any) with what has been loaded in the
    * foreign global scope (_global).
    */
   function syncContext(context, _global) {
       for(var k in _global) {
           if (k !== CTX_PROPERTY_NAME) {
               context[k] = _global[k];
               var sb = context._sandbox;
               if (sb) {
                   sb[k] = context[k];
               }
           }
       }
   }

   /**
    * The passed sandbox is used as a global context.
    * With nashorn we can't provide a global instance. We are keeping a reference
    * to the sandbox and feed it with content loaded in the foreign context.
    * This is done in syncContext function
    */
   exports.NodeScript.prototype.runInNewContext = function(sandbox) {
       var context = createNewContext(sandbox);
       return this.runInContext(context);
   }

   exports.NodeScript.runInNewContext = function(code, sandbox, filename) {
       var context = createNewContext(sandbox);
       return exports.NodeScript.runInContext(code, context, filename);
   }

   function createNewContext(sandbox) {
       var context = {};
       addGlobalScope(sandbox, context);
       if (sandbox) {
           // Keep a reference on the sandbox, will be used to feed sandbox
           // with new definitions. This is done in syncContext
           context._sandbox = sandbox;
           Object.defineProperty(context, '_sandbox',  { writable: false,  configurable: false, enumerable: false});
       }
       return context;
   }

});
