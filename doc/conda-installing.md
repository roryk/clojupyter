# Conda-installing Clojupyter

Table of Contents

* [Quickstart](#quickstart)
* [Using Conda: Building Clojupyter](#using-conda-building-clojupyter)
* [Using Conda: Installing Clojupyter](#using-conda-installing-clojupyter)
* [Using Conda: Using Environments](#using-conda-using-environments)
* [Using Conda: Upgrading Clojupyter](#using-conda-upgrading-clojupyter)
* [Using Conda: Uninstalling Clojupyter](#using-conda-uninstalling-clojupyter)
* [Supported Platforms when using Conda (Linux, MacOS, Windows)](#supported-platforms-when-using-conda)

Installing Clojupyter using Conda is the easiest way to get started with Clojupyter, but it does
have some limitations, you may want to consult [Clojupyter Usage Scenarios](usage-scenarios.md)
understand your options and the pros and cons of each of them.

## Quickstart

The Quickstart way to install Clojupyter using Conda means doing 2 things:

1. Install [Anaconda](https://anaconda.com)
1. Install Clojupyter using the package management tool `conda` which is included in Anaconda

### Installing Anaconda

Go to the [Anaconda web site](https://www.anaconda.com), Click
[Download](https://www.anaconda.com/distribution/), select your preferred platform, and download the
`Python 3.7` edition of the Anaconda Distribution.


### Installing Clojypyter

Once Anaconda is installed you use the included `conda` package management tool to install
Clojupyter from Anaconda Cloud.  Exactly how you run `conda` depends on the platform you are using,
consult the Anaconda documentation.

With access to `conda` installing Clojupyter (which is available on the `simplect` channel in
Anaconda Cloud) is straightforward:

```
> conda install -y -c simplect clojupyter
...elided...
Successfully installed Clojupyter into ~/anaconda3/share/jupyter/kernels/conda-clojupyter.\
\n\nexit(0)\n'
done
>
```

This should work on all supported platforms: Linux, MacOS and Windows.

More details on installing below.

## Using Conda: Building Clojupyter
To build a conda package run:
```bash
$ make conda-build
```

To build a custom distribution of Clojupyter, run `make conda-config` to build the prerequisites, but without running `conda build`.
At this point you can add any extra files to the package, update the meta file and/or the build
scripts and run either `conda build conda` or `make conda-build`.

## Using Conda: Installing Clojupyter

You use the `conda` command to manage Conda environments and packages; this obviously includes
Clojupyter.  Conda is a full-featured package manager, in this document we focus on the very limited
set of things you need to know to manage a conda-install instance of Clojupyter.  There are features
in conda which might be of interest to Clojupyter users (among other things conda allows you to
manage 'environments' which enable you have multiple versions of your installed software; this can
be used to switch between different version of Java, Clojure and Clojupyter), but we believe most
Clojupyter users with more advanced requirements will want to switch to self-managed kernels and so
we'll not spend much time on the possibilities and features of Conda.  If you are interested [see
the technical documentation conda](https://docs.conda.io/projects/conda/en/latest/index.html).There
is additional documentation in the following section titled
[Using Conda: Using Environments](#using-conda-using-environments) which will help guide you through
some of the caveats you may run into, but it is not expected to be comprehensive. You may wish to
jump ahead and familiarize yourself with that first before proceeding if you plan on using
environments, but it is considered optional and the following will still apply.

To install Clojupyter, make sure your conda package build succesfully and run `conda install --use-local clojupyter`.

Conda organizes software into **packages** which have **versions** which can be built and deployed
multiple times with a **build number**, and delivers it on **channels**.

You can install a package simply by specifying the package name, or be more specific and indicate
which version and/or build number you want to install.  The way to express the package/version/build
is known as a **package_spec**.

To install the most recent version of Clojupyter:

```
> conda install -c simplect clojupyter
```

To install a specific version (giving you the build with the highest build number):

```
> conda install -c simplect clojupyter=0.2.3snapshot
```

To install a specific build of a specific version (here: build number 2):

```
> conda install -c simplect clojupyter=0.2.3snapshot=2
```

Conda will normally prompt you to confirm the action it is about to take; you can tell it to skip
prompting using the `-y` flag.

Using both version and buildnum enables you tell conda to go back and forth between versions as
needed, including *downgrades* (note the warning from conda in the middle of the output):

```
> conda list | grep clojupyter
clojupyter                0.2.3snapshot                 2    simplect

> conda install -y -c simplect clojupyter=0.2.3snapshot=1
Collecting package metadata (repodata.json): done
Solving environment: done

## Package Plan ##

  environment location: ~/anaconda3

  added / updated specs:
    - clojupyter==0.2.3snapshot=1


The following packages will be DOWNGRADED:

  clojupyter                                0.2.3snapshot-2 --> 0.2.3snapshot-1


Preparing transaction: done
Verifying transaction: done
Executing transaction: \ b'Clojupyter v0.2.3-SNAPSHOT@ ... \
Successfully installed Clojupyter into ...
done

> conda list | grep clojupyter
clojupyter                0.2.3snapshot                 1    simplect
>
```

In the `conda list` output the columns left to right are 'Package Name', 'Package Version', 'Package
Build Number', and 'Channel Name'.

Conda will occasionally want to upgrade other components than Clojupyter (including conda itself)
when you use package management commands related to Clojupyter.  If you want to control how conda
upgrades components and dependencies consult the `conda` technical documentation for the relevant
command.

## Using Conda: Using Environments

The proceeding section installs Clojupyter to the base environment. This is probably sufficient to
familiarize yourself with Clojupyter, especially if you are new to using Conda, but if you use Conda
already for different languages and already use environments, this is probably not desirable. The
command `conda env list` will show you the environments currently configured.

```
> conda env list
# conda environments:
#
base                  *  /path/to/conda
```

To create a new environment use the command `conda create --name <env-name>`.

```
> conda create --name clojupyter
Collecting package metadata (current_repodata.json): done
Solving environment: done
## Package Plan ##
  environment location: /path/to/conda-env/clojupyter
Proceed ([y]/n)? y
Preparing transaction: done
Verifying transaction: done
Executing transaction: done
#
# To activate this environment, use
#
#     $ conda activate clojupyter
#
# To deactivate an active environment, use
#
#     $ conda deactivate
> conda env list
# conda environments:
#
base                  *  /path/to/conda
clojupyter               /path/to/conda-env/clojupyter
```

Then use the command `conda activate <env-name>` to switch to the environment anytime you want to
select it:

```
> conda activate clojupyter
```

At this point follow the installation steps already provided. If you switch to this environment and
start Jupyter, the Clojupyter kernel will be available. However, one of the more flexible benefits
of using Conda environments is that you can isolate dependencies and make it possible to run
different kernels side-by-side. If you are already using Jupyter with a Python kernel for instance,
you may want to have a separate Jupyter environment and expose the different kernels there. This has
the convenience of being able to use one environment for Jupyter but being able to switch to
different kernels on-demand for different Notebooks.

This capability may not be working fully as intended, so if you run into problems, please document
them and open a report. For this scenario, it is assumed that you have 3 environments configured:

```
> conda env list
# conda environments:
#
base                  *  /path/to/conda
clojupyter               /path/to/conda-env/clojupyter
jupyter                  /path/to/conda-env/jupyter
```

Assuming you have jupyter configured as your JupyterLab environment and clojupyter configured as
where you have installed Clojupyter, how do you expose the clojupyter kernel to the Jupyter
environment? This is done using the `jupyter kernelspec` command:

```
> conda activate jupyter
> jupyter kernelspec install /path/to/conda-env/clojupyter/share/jupyter/kernels/conda-clojupyter --user
[InstallKernelSpec] Installed kernelspec conda-clojupyter in /path/to/conda-user-env/jupyter/kernels/conda-clojupyter
```

You may need to adjust the path to get the correct location depending on your OS, but in the
environment directory you should find something similar. Look at the help for `jupyter kernelspec`
for more information. You can also make adjustments to the `kernel.json` file or pass additional
arguments to adjust how the kernel is installed.

If you try running this kernel right now, it will fail with a rather cryptic error message. While
Jupyter doesn't require it, Clojupyter requires OpenJDK to be installed. This must be added to the
Jupyter environment which loads the Clojupyter kernel.

```
> conda install openjdk
Collecting package metadata (current_repodata.json): done
Solving environment: done
## Package Plan ##
  environment location: /path/to/conda-env/clojupyter
  added / updated specs:
    - openjdk
The following NEW packages will be INSTALLED:
  openjdk            pkgs/main/win-64::openjdk-11.0.6-he774522_1
Proceed ([y]/n)?
Preparing transaction: done
Verifying transaction: done
Executing transaction: done
```

At this point, you should be able to open JupyterLab and see Clojupyter available next to any other
kernels you may have in your Jupyter environment.

## Using Conda: Upgrading Clojupyter

Conda enables you upgrade packages using the `conda` subcommand `update`:

```
> conda list | grep clojupyter
clojupyter                0.2.3snapshot                 1    simplect

> conda update -c simplect clojupyter
Collecting package metadata (repodata.json): done
Solving environment: done

## Package Plan ##

  environment location: ~/anaconda3

  added / updated specs:
    - clojupyter


The following packages will be UPDATED:

  clojupyter                                0.2.3snapshot-1 --> 0.2.3snapshot-2


Proceed ([y]/n)? y

Preparing transaction: done
Verifying transaction: done
Executing transaction: \ b'Clojupyter v0.2.3-SNAPSHOT@cd18-DIRTY - Conda Unlink\n\n    0 files found in ~/anaconda3/share/jupyter/kernels/conda-clojupyter.\n    Conda unlink completed successfully.\n\nexit(0)\n'
- b'Clojupyter v0.2.3-SNAPSHOT - \n\n    Successfully installed Clojupyter into ~/anaconda3/share/jupyter/kernels/conda-clojupyter.\n\nexit(0)\n'
done
>
```

Conda update will upgrade your installed Clojupyter to the highest build number of the highest
version.  If you want something other than, use `conda install` and use the package_spec to specify
exactly which version and build number you want.

## Using Conda: Uninstalling Clojupyter

You use the `conda` subcommand `remove` to uninstall Clojupyter:

```
> conda list | grep clojupyter
clojupyter                0.2.3snapshot                 2    simplect

> conda remove clojupyter
Collecting package metadata (repodata.json): done
Solving environment: done

## Package Plan ##

  environment location: ~/anaconda3

  removed specs:
    - clojupyter


The following packages will be REMOVED:

  clojupyter-0.2.3snapshot-2
  maven-3.6.0-0


Proceed ([y]/n)? y

Preparing transaction: done
Verifying transaction: done
Executing transaction: \ b'Clojupyter v0.2.3-SNAPSHOTY - Conda Unlink\n\n    \
0 files found in ~/anaconda3/share/jupyter/kernels/conda-clojupyter.\n    \
Conda unlink completed successfully.\n\nexit(0)\n'
done
>
```

## Supported Platforms when using Conda

Anaconda Cloud supports a number of platforms, the list includes `linux-32`, `linux-64`
`linux-aarch64`, `linux-armv6l`, `linux-armv7l`, `linux-ppc64le`, `osx-64`, `win-32`, and `win-64`.

At this time Clojupyter's conda platform support comprises `linux-64`, `osx-64` and `win-64`.  It
should be fairly straightforward to support `linux-32` and `win-32` (has not been tried), support
for other of the above platform might be harder.
