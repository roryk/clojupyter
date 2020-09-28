# Clojupyter Plugins

Starting with version 0.4.0, Clojupyter supports runtime extensibility by
means of **plugins**.

A plugin is simply a jar package that can, but doesn't have to, depend on
the clojupyter kernel.

## Getting Started

Build and install the kernel:
```bash
$ make install
```
To confirm that the kernel installed correctly, run:
```bash
$ bin/list
```
If you check the kernel path, you'll notice that the jar is no longer there,
but is installed in a different location, depending on the host OS.
To see the actual path of the jarfile, check the kernel.json:
```bash
$ cat <kernel path>/kernel.json
```

## Library Directory
Here is the typical structure of a Clojupyter installation with two enabled plugins:
```bash
$ tree <kernel lib path>
<kernel lib path>
├── clojupyter-0.4.0.jar
├── lib
│   ├── algo.generic-0.1.3.jar
│   ├── antlr4-4.5.3.jar
│   ├── antlr4-runtime-4.5.3.jar
│   ├── arrangement-1.1.1.jar
....... [many other deps]
│   ├── clojupyter-0.4.0.jar -> ../clojupyter-0.4.0.jar
....... [many other deps]
│   ├── leaflet-0.1.0.jar -> ../plugins/leaflet-0.1.0.jar
....... [many other deps]
│   ├── whidbey-2.1.0.jar
│   ├── widgets-0.1.0.jar -> ../plugins/widgets-0.1.0.jar
│   └── zprint-1.0.0.jar
└── plugins
    ├── enabled
    │   ├── leaflet-0.1.0.jar -> ../leaflet-0.1.0.jar
    │   └── widgets-0.1.0.jar -> ../widgets-0.1.0.jar
    ├── leaflet-0.1.0.jar
    └── widgets-0.1.0.jar

3 directories, 116 files
```
In order to make sense of this arrangement, we need two differentiate between two types of jar files.

All the jars in `lib` are **thin jars**, the same jars that you find on *clojars* or *maven*.

The clojupyter jar file is different, because it also includes references to
its dependencies in its *MANIFEST.MF* file. We'll call that **annotated jars**.
Likewise, the jars in `plugins` are annotated.

Symlinks in `lib` are created to allow other packages to depend on annotated jars, without making a whole new copy of the jars on disk.

Jars on `plugins/enabled` are automatically included on classpath at runtime.
`bin/enable-plugin` does that by creating symlinks in `enabled` directory.

## Shell Scripts
To manage the kernel and its plugins, Clojupyter includes bash and powershell
scripts. These include facilities for **installing**, **uninstalling**, **listing**, **enabling** and **disabling plugins**.
**Enable** and **Disable plugins** resolve the plugins dependencies, so they expects an annotated jar in `plugins`.

E.g: Let think about two clojupyter plugins: `foo` and `bar`,with `foo` depending on `bar`.
If both are disabled and you call `bin/enable-plugin foo`, the scipt will enable both `foo` and `bar`.
Likewise, if both are enabled and you call `bin/disable-plugin bar` will disable both `foo` and `bar`.

**Note:** The arguments you pass to `enable-plugin`, `disable-plugin` and
`uninstall` are regex patterns used to match the name of the kernel/plugin.
Thus, you can enable all the installed plugins with `bin/enable-plugin .`.
To force an exact match, use `bin/enable-plugin ^foo-0.1.0$`.

## Installing Plugins
Plugins are expected to install and uninstall themselves. A properly installed plugin is one that:
* copies the **annotated jar** in `<kernel lib path>/plugins`
* copies its dependencies to `<kernel lib path>/lib`, while skipping existing files.
* makes a symlink in `lib` to the annotated jar in `plugins`.

## Creating Plugins
To get started with a plugin project, run `lein new clojupyter-plugin`.
This template includes the shell scripts to install and uninstall the
plugin as well as the necessary tools to build the **annotated jar**.

For a reference plugin, check out the [widgets](https://github.com/nighcoder/widgets) project.
