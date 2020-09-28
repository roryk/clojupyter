# Changelog

## v0.4.0 - Sept 2020
* New building strategy to allow Clojupyter to share the runtime with third party
plugins.
* Updated `docker` and `conda` recipes.
* Updated the Makefile with new targets and recipes.
* Clojupyter now respects the `CLASSPATH` environment variable, making it possible
to run jupyter notebooks in the context of a clojure project managed by leiningen or clojure.
* Updated `clojupyter.kernel.comm-atom` namespace
    * Removed agents from the CommsAtom model
    * The CommsAtom state is held in a clojure atom
    * Updated `comm-atom-proto` to three methods: `sync-state`, `close!` and `send!`
    * Added handler for `comm-msg-custom`.
    * Added new tests for **comm-msg**
* Removed `clojupyter.cmdline` interface.
    * Removed tests and dependencies of the `cmdline` interface.
* Removed namespaces: `clojupyter.misc.display`, `clojupyter.kernel.version`, `clojupyter.kernel.logo`
    * `clojupyter.misc.display` content moved to `clojupyter.display`.
    * `clojupyter` namespace now contains `logo` and `version` and `license`
    (Note the missing \*earmuffs\*, the vars are no longer dynamic)
* `clojupyter.display/hiccup-html` renamed to `hiccup`
* `clojupyter.display/html` now only renders html string. To render hiccup data, use `hiccup`
* New display functions: `vega`, `vega-lite`, `gif`, `pdf`, `json`
* Removed usage of `deps.edn`. Clojure (the executable) does not work on Windows, so
relying on it to build the package breaks anyone interested in building on Windows.
* Bumped depencency versions for remaining deps.
* Added bash/powershell scripts to manage Clojupyter kernels and plugins.


## v0.3.2

* Reintroduce AOT compilation of `clojupyter.cmdline` (unintentionally dropped in transition to
  `tools.deps` build tool chain), in  `project.clj`

## v0.3.1

* Add explicit dependency on `jupyterlab` and `qtconsole` to conda install

## v0.3.0

* Fix issue #105: "Windows: Can't interrupt the kernel when stuck in an infinite loop"
* Fix issue #104: "Clojupyter Jupyter kernelspec is not installed in non-base conda environment"
* Fix issue #103: "Syntax error on `make install` (Imagemagick needed to install Clojupyter)" (functionalty removed)
* Fix issue #94: "Specifying dependencies in Clojupyter with deps.edn"
* Add support for data dir (history file) on Windows
* Add support for configuration files on Windows
* Add Docker files for building Clojupyter in `./docker` (for reference).
* Switch to message-based interrupts including support for interrupts on Windows, eliminate
  dependency on `beckon`
  * Switch to 2 event-handling threads, one for `:control_port` and one for `:shell_port` with only
    interrupts and shutdown messages being handled on `:control_port`
  * Add handling of `INTERRUPT_REQUEST` messages
* Remove icon customization options `--icon-top`, `--icon-top`, and `--customize-icons`
* Add `comm_atom` representing the state of COMM objects synchronized with the Jupyter COMM model
  manager
* Simplify `cljsrv.clj`
* Implement support for `COMM` messages, necessary for interactive widget support
* Use `deps.edn`, `tools.deps` and `lein-tools-deps`, update `project.clj` and `.travis.yml`
  accordingly
* Change multithreading model from 2 independent (and mutually ignoring) threads to threads handling
  ZeroMQ and a kernel threads
* ZeroMQ communication reimplemented and moved to clojupyter.zmq*, including
  * Elimination of potential race condition on ZeroMQ sockets, necessary to support multithreading
  * Move to jeromq `ZContext`-based implementation using `ZMQ$Socket`
* Kernel communicates using `core.async` channels
* Move to `io.pedestal.interceptor` gaining flexibility, higher degrees of pure functions, and
  possibility of asynchronuous event handling.  Remove middleware model.
* Improved format of logging, introduction of `slf4j-timbre` to capture logging from interceptors
* Specification of all Jupyter protocol messages using `clojure.spec`
* Test coverage significantly expanded including generators for all Jupyter protocol messages
* nrepl communication reimplemented incresing use of pure functions
* Use Latin function names in io.simplect.compose
* Use Clojure icon
* Various dependencies updated


## 2019-11-26
* Merged hotfix for [Issue #104: Clojupyter Jupyter kernelspec is not installed in non-base conda
  environment](https://github.com/clojupyter/clojupyter/issues/104)
* Merged hotfix/workaround for [Issue #103: syntax error on `make install` / Imagemagick needed to
  install Clojupyter](https://github.com/clojupyter/clojupyter/issues/103)

## v0.2.3

* Add support for `conda install` of Clojupyter supporting installs on Linux, MacOS, and Windows
  * Make implementation of installation more flexible to support both local installs, building Conda
    packages, and installing the Clojupyter conda package on end-user machine
  * Eliminate all Unixy dependencies as shell scripts, use Java and Clojupyter jarfile for all
    build/install/uninstall operations
  * Build Conda packages for Linux, MacOS, and Windows
  * Install and remove Clojupyter on end-user machines directly using Conda on Linux, MacOS, and Windows.
    * Install: `conda install -c simplect clojupyter`
	* Remove: `conda remove clojupyter`
* Add support for using Clojupyter as a library where resulting standalone jar knows how to install
  itself as a Jupyter kernel
  * Covers part of the functionality of `lein-jupyter` without dependency on Leiningen
* Improve and extend command line interface
  * Control the kernel identifier shown in Jupyter Lab and Notebook
  * List, remove, and install Clojupyter kernels using the command line interface
  * Build an application and use the Clojupyter command line interface to install and run it
  * Get version info
  * Remove dependency on `lein` but maintain compatibility
* Minor
  * Regularize version info and interface
  * Move resources to `./resources/clojupyter` to reduce risk of conflicts when Clojupyter is used
    as a library
  * Distinguish more clearly between pure and impure functions
    * In new code (`clojupyter.install.*`): Side-effecting functions located in `*_actions.clj` files
	* In new code (`clojupyter.install.*`): `io.simplect.compose.action` objects reify inspectable
      actions, enabling property-based testing of logic
  * Add test.check-based tests for clojupyter.install.*
  * Details
    * Regularize throws to use clojupyter.util-actions/throw-info
	* `project.clj`: remove `:keep-non-project-classes true`, does not appear we need it
* No functional changes to kernel

## v0.2.2

* [x] Enhancement: **Upgrade to nREPL v0.6** - eliminate uncaught stacktrace error
* [x] Introduce **modular message handling** incl new tests
* [x] Add retrievable version including timestamp, printed at startup (banner)
* [x] Add **config file** to control features such as log level
* [x] Address (some) **reported issues**
  * `#83: Hanging after evaluating cell with syntax error`
  * `#84: Kernel dies on empty code string`
  * `#30: Dead kernel on startup (comm_info_request)` (don't terminate on unknown messages)
  * `#69: FileNotFoundException Could not locate... on classpath.`
  * `#76: add-javascript broken?`
  * `#58: AOT+old tools.reader causing problems using latest ClojureScript`
  * `#85: Consider the silent and store_history keys of an execute request`
* [x] **Improve basic install**
  * [x] Eliminate **shell script in `clojupyter` install artifact** (call java directly)
  * [x] Add **clojupyter icons** (in Jupyter Lab Launcher, top right corner in Jupyter Notebook)
  * [x] Add **version numbered icons**
  * [x] **Replace shell scripts** with lein functions for basic build operations
    * `clojupyter-install`
    * `check-os-support`
    * `check-install-dir`
    * `update-version-edn`
  * [x] Add **version-specific kernel install directory**
  * [x] Make **default kernel install directory `clojupyter`** (instead of `clojure`)
  * [x] Add `make` targets
    * `update-version-edn`
    * `install-version` (install plain icon)
    * `install-version-tag-icons` (install version-tagged icon, depends on `convert` from  `imagemagick` package)
* [x] Create **updated Docker image** for current version
* [x] Update TODO in `README.md`: add tentative near-term roadmap
* [x] Clean out obsolete issues on github
* [x] Delete unused branches on github
* [x] Establish numbered releases
* [x] Update example notebooks
* [x] Change code structure towards having an **open, extensible architecture** (more work is needed to make it
  extensible)
  * [x] Introduce **handler-based middleware structure** similar to [nREPL](https://nrepl.org) and
    [Ring](https://github.com/ring-clojure/ring)
  * [x] Enable higher degree of **separation of concerns**, thus enabling adding features in a
    modular fashion
  * [x] Introduce **layered message handling**: Incoming messages pass through a define sequence of
    middleware handlers with outbound message passing through the same set of handlers in
    reverse order, yielding a layered message-processing architecture
  * [x] Introduce  `handler`, `middleware`, and `transport` abstractions similar to those of
    [employed in nREPL](https://nrepl.org/nrepl/0.6.0/design/transports.html), but adapted
    to fit into the multichannel context of Jupyter (cf. `send-stdin`, `send-iopub`, and
    `send-req` in `clojupyter.transport`)
  * [x] **Convert existing functionality to middleware-based structure**, messaging-handling is now defined
    by core message handler (in `clojupyter.middleware`):

    ```
    (def wrap-base-handlers
      (comp wrap-execute-request
            wrap-comm-msg
            wrap-comm-info
            wrap-comm-open
            wrap-is-complete-request
            wrap-complete-request
            wrap-kernel-info-request
            wrap-inspect-request
            wrap-history-request
            wrap-shutdown-request))

    (def wrap-jupyter-messaging
      (comp wrapout-encode-jupyter-message
            wrapout-construct-jupyter-message))

    (def default-wrapper
      (comp wrapin-verify-request-bindings
            wrapin-bind-msgtype
            wrap-print-messages
            wrap-jupyter-messaging
            wrap-busy-idle
            wrap-base-handlers))

    (def default-handler
      (default-wrapper not-implemented-handler))
    ```
    The modular structure will make it much easier to add functionality going forward.

  * [x] **Reorganize code structure**:
  ```
  src
  ├── clojupyter
  │   ├── display.clj
  │   ├── javascript
  │   │   └── alpha.clj
  │   ├── kernel
  │   │   ├── cljsrv
  │   │   │   ├── nrepl_comm.clj
  │   │   │   ├── nrepl_middleware.clj
  │   │   │   └── nrepl_server.clj
  │   │   ├── config.clj
  │   │   ├── core.clj
  │   │   ├── history.clj
  │   │   ├── init.clj
  │   │   ├── jupyter.clj
  │   │   ├── logo.clj
  │   │   ├── middleware
  │   │   │   ├── base.clj
  │   │   │   ├── comm.clj
  │   │   │   ├── complete.clj
  │   │   │   ├── execute.clj
  │   │   │   ├── history.clj
  │   │   │   ├── inspect.clj
  │   │   │   └── log_traffic.clj
  │   │   ├── middleware.clj
  │   │   ├── spec.clj
  │   │   ├── stacktrace.clj
  │   │   ├── state.clj
  │   │   ├── transport
  │   │   │   └── zmq.clj
  │   │   ├── transport.clj
  │   │   ├── util.clj
  │   │   └── version.clj
  │   ├── misc
  │   │   ├── display.clj
  │   │   ├── helper.clj
  │   │   ├── leiningen.clj
  │   │   └── mime_convertible.clj
  │   └── protocol
  │       └── mime_convertible.clj
  └── clojupyter.clj

  8 directories, 32 files
  ```
* [x] Add support for **configuration file**, loaded at startup
  * [x] Read from `~/Library/Preferences/clojupyter.edn` on MacOS and `~/.config/clojupyter.edn` on
  Linux (per XDG Base Directory specification)
  * [x] Move location history file to `~/Library/Caches` on MacOS and `~/.local/share` (per XDG Base
    Directory specification)
  * [x] Control printing of stacktraces (workaround, base issue addressed by upgrade to nREPL 0.6) in configuration file
  * [x] Control log level in configuration file
  * [x] Control ZMQ traffic logging in configuration file
* [x] **Improve tests**
  * [x] Convert existing tests to work with new structure
  * [x] Add tests for middleware providing checks for basic message responses
* [x] Miscellaneous
  * [x] Upgrade Jupyter messing protocol to v5.3 (from v5.0) - no apparent impact
  * [x] Replace `clj-time` with `clojure.java-time`
  * [x] Rename `idents` in ZMQ messages to `envelope`
  * [x] Define names for Jupyter message identifiers (in `clojupyter.misc.jupyter`)
  * [x] Eliminate `zmq-comm` protocol - not needed
  * [x] Use `:aot` only in `uberajr` profile in `project.clj`

## v0.2.1

* [x] Improve code structure / organization
  * [x] **Use a single atom for all global state.** Provide abstraction for manipulating
    global state.  Eliminate `states`.
  * [x] Use **accessor functions for socket access**. Avoids storing sockets in `Zmq_Comm`.
   Accessor functions for `stdin-socket` and `iopub-socket` in passed-around map `S`.
  * [x] Eliminate atoms containing sockets - not necessary since they are used, not updated.
  * [x] Remove depencency on `async.core` - not used.   
* [x] `core.clj`
  * [x] Replace `configure-shell-handler` and `configure-control-handler` with `make-handler`.  Their
   role is the same, the main difference that `execution-counter` is incremented on the shell
   socket.  Why not simply use the same handler?
  * [x] Integrate creation of signer and checker functions.
  * [x] Simplify `run-kernel` based on above.
  * [x] Separate code related to `nrepl-server` and Clojupyter middlerware into separate namespace:
   `clojupyter.misc.nrepl-server`.
* [x] `messages.clj`
  * [x] Considerably simplify `make-shell-handler` and `make-control-handler`: Unify into one.
  * [x] Use consolidation of global state to turn `handle-execute-request` into regular responding
   function defined with `defresponse`.
  * [x] Add `with-debug-logging`
  * [x] Add accessor functions for socket access.
  * [x] Integrate creation of signer and checker.
  * [x] Integrate `status-content` and `pyin-content` into point-of-use.
  * [x] Use abstractions to access global state.
* [x] `nrepl_comm.clj`  
   * [x] Refactor giant `defrecord` into smaller functions, no change in logic.
* [x] `history.clj`
   * [x] Move determination of max history length here.
* [x] `nrepl_server.clj`(added)
   * [x] `nrepl`-related code from `core.clj`.
* [x] `state.clj`
   * [x] Rename from `states.clj` - add single atom for all of global state.
   * [x] Add functions to manipulate global state.
* [x] `zmq_comm.clj`
   * [x] Use accessor functions instead of access-by-socket-name.
* [x] `util.clj`
   * [x] Add `with-debug-logging` macro.
   * [x] Add `reformat-form`.  Very early experiment with auto-indent / auto-reformat of cells based on
     [Code Prettify](https://jupyter-contrib-nbextensions.readthedocs.io/en/latest/nbextensions/code_prettify/README_code_prettify.html)
     / [Autopep8](https://github.com/kenkoooo/jupyter-autopep8). Very primitive / early cell reformatting
     prototype appears to work (cf. `./nbextensions/code_prettify/autopep8.yaml`). More work needed (quite a bit),
     but it looks like there's a fairly straightforward solution.
* [x] `user.clj`
  * [x] Added to support experiments with `reformat-form`.

Changes at revision `2913dc17` relative to Clojupyter `master` latest commit per 15 February 2019 (`994f680c`):

* [x] Ensure compatibility with **Clojure v1.9** and **Clojure v1.10**
* [x] Enable **`nrepl`-based access** from interactive development environments such as CIDER/Cursive:
  Leave `:value` unchanged, add `:mime-tagged-value` instead
* [x] **Update dependencies**, not least move to `nrepl` v0.5.3 and `cider-nrepl` v0.20.0
* [x] Improve code structure / organization
  * [x] Use a map for passing largely unchanging values around
  * [x] Abstract message content access: Use `message-*` functions
  * [x] Make functions used only locally `:private`
* [x] `core.clj`
  * [x] Refactor `configure-{shell,control}-handler` to use multi-method instead (`respond-to-message`)
  * [x] Use macro `catching-exceptions` to improve code readability
  * [x] Refactor `process-heartbeat` away
  * [x] Shorten arglists using a map instead of individual args
  * [x] Add `nrepl-server-addr` to retrieve `nrepl` server port to enable connection from interactive
   development environment
* [x] `messages.clj`
  * [x] Use multi-method `respond-to-message` dispatching on request type to eliminate individually
    named response functions such as `input-request`, `comm-open-reply`, `kernel-info-reply`, etc.
  * [x] Use macro `defresponse` to further reduce code size
  * [x] Refactor `shutdown-reply` separating shutdown actions and responding to shutdown request
  * [x] Refactor away content-calculating functions to get better locality permitted by much smaller response
    generating functions
  * [x] Refactor `send-message` and `send-router-message` to reduce code redundancy and make code easier to follow
  * [x] Refactor `execute-request-handler` a bit, add comments
  * [x] Rename `get-message-{signer,checker}` to `make-message-{signer,checker}`
  * [x] Move some generic helper functions to `util.clj`
* [x] `middleware/mime_values.clj`
  * [x] Assoc `:mime-tagged-value` to result of `to-mime` (instead of mime-tagging `:value`
    directly): Enables using the `nrepl` server using regular clients
* [x] `protocol/mime_convertible.clj`
  * [x] Print `nil` as `nil` (instead of not printing anything)
* [x] `stacktrace.clj`
  * [x] Add mechanism to control whether stacktraces are printed or not as it appears
    that `cider-nrepl` occasionally triggers uncaught exceptions. Most likely linked to upgrade of
    `nrepl` and/or `cider-nrepl`; the cause of the problem not understood as yet.
* [x] `core_test.clj`
  * [x] Update tests
* [x] Miscellaneous code cleanup
  * [x] Send error messages to Jupyter stream `stderr` instead of `stdout`
  * [x] Use `->Class`-forms, e.g. `->HiccupHTML` instead of `HiccupHTML.`, to allow interactive class updates
  * [x] Extra `log/debug` here and there
  * [x] Various reformatting here and there
* [x] Examples
  * [x] `html-demo.ipynb`
    * [x] Update to use Vega Lite instead of Highcharts (which seems to be broken in both updated version
     and current `HEAD` of `master` using Clojure 1.8)
  * [x] `incanter-demo.ipynb`
    * [x] Update to print Clojure version
