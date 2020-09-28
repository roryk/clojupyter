# Usage Scenarios

Here you can read about different ways of using Clojupyter (Cλ).

Scenario overview:

| Scenario    | What                                           | Description                              | Recommended?
|-------------|------------------------------------------------|------------------------------------------| -----
| **STANDALONE**| Build and install clojupyter on your computer | Run Clojupyer natively.                  | Y
| **CONDA**    | Simply do `conda install ...` to get started   | Easiest way to get started with Cλ       | Y
| **DOCKER**   | Quickly check out Cλ                           | Try Cλ without installing anything       | N


Details of each scenario described below.

## STANDALONE: Build and install clojupyter on you computer
If you already have `Jupyter` installed, you can install Clojupyter alongside your other existing kernels.
Clojupyter build, installs and runs natively on **Linux**, **MacOS** and **Windows**.

`bin` directory contains bash and powershell scripts to manage the native installations of clojupyter,
including but not limited to: *installing*, *uninstalling*, *listing*, *enabling* and *disabling plugins*.

The **STANDALONE** and **CONDA** scenarios allow you to use the Clojupyter kernel in the context of
a clojure project by passing the `CLASSPATH` environment variable to the jupyter process.
E.g.: Let's imagine a clojure project `foo` that includes `project.clj` in its root directory.
If you call `CLASSPATH=$(lein classpath) jupyter <lab/notebook>`, you'll start a jupyter lab/notebook
process with the source code of `foo` and its dependencies available to Clojupyter at runtime.
If you use `deps.edn` for dependency management, you can get a list of deps with `clojure -Spath`

`bin/clojupyter` does the `CLASSPATH` setup automatically, so it can be copied somewhere on the `PATH`
and be used to start jupyter when working with clojure projects.

## CONDA: Use `conda` to install Clojupyter on Linux, MacOS and Windows

Clojupyter supports [Anaconda](https://www.anaconda.org) which is the installation method
[recommended by the Jupyter project](https://jupyter.readthedocs.io/en/latest/install.html#installing-jupyter-using-anaconda-and-conda).
If you are new to Clojupyter and/or Clojure, we recommend that you use this method to install
Clojupyter.

Installing Clojupyter using Anaconda gets you a generic Clojupyter kernel providing essentially
unlimited functionality.  One of the key advantages to the Conda install is that, by leveraging the
infrastructure provided by [Anaconda](https://www.anaconda.org), it provides a very simple way to install
Clojupyter on both **Linux**, **MacOS** and **Windows**: All you need to do is (1) install
Anaconda and (2) use a single command to install Clojupyter.

See [Conda-installing Clojupyter](conda-installing.md) for details on how to install Clojupyter
using Anaconda.


Usage scenario **CONDA** overview:

| **FEATURE**             | **COMMENT**                                               |
|-------------------------|-----------------------------------------------------------|
| Headline                | Easiest way to get started with Clojupyter                |
| Audience                | End-users                                                 |
| What it gets you        | Generic Clojure/Clojupyter                                |
| How to install          | `conda install -c simplect clojupyter`                    |
| Supported platforms     | Linux, MacOS, Windows (all 64 bit)                        |
| Disadvantage            | No custom code in kernel - must be loaded in each session |


## DOCKER: Try Clojupyter via a Docker image

If you have [Docker](https://www.docker.com/) and you simply want to try out Clojupyter without
installing anything, you can run the pre-built Clojupyter Docker image.  It has the full
functionality of Clojupyter, but since it is not well integrated into the host environment (it can
only 'see' notebooks in a certain sub-directory, for example), we **do not recommend** that you use
Clojupyter this way if you want to do real work - any of the other (above) ways of accessing
Clojupyter should be preferable to running it inside Docker.  For end-users new to Clojupyter we
recommend using Anaconda to install a generic Clojupyter (see CONDA scenario above) instead.  As you
gain experience you may want to install your own custom kernel, with additional libraries and
functionality preloaded (see LIB scenario above).

See [The Docker Way](docker.md) for details on how to try out Clojupyter using Docker.

Usage scenario **DOCKER** overview:

| **FEATURE**             | **COMMENT**  |
|-------------------------|--------------------------------------------|
| Headline                | Try Clojupyter without installing anything (requires Docker)  |
| Audience                | Prospective Clojupyter users                                  |
| What it gets you        | Ability to evaluate Clojure code in Jupyter                   |
| How to install          | See Installation guide                                        |
| Supported platforms     | Same as Docker                                                |
