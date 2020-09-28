# Clojupyter and Conda

[Anaconda](https://anaconda.org) is the recommended way to install Jupyter and so we can reasonably
assume that many or most Jupyter users have Anaconda on their machine.  Clojupyter includes Conda
support to make it easier for Jupyter users to get started with Clojupyter/Clojure.

## Core Conda Concepts at a high level

Documentation for building Conda packages can be
[here](https://docs.conda.io/projects/conda-build/en/latest/index.html).

At a high level Conda enables you to create a reusable *recipe* for building a package.  A
**package** is anything you install using your package manager.  A conda package is a compressed
tarfile containing the module to be installed plus instructions on how to install it.  You use
`conda build` to build a package.  Conda **channels** contain packages and conform to a standard
structure and contain an index of available packages.  Conda is able to install from channels and
uses the indexes in the channel to solve for requirements and dependencies.  A conda package has a
**version number** and a **build number** which enable you to build versions multiple times
and keep them distinct from a package management point of view.

## Clojupyter's use of Conda

Clojupyter's use of Conda is rudimentary at best, sufficient to get started distributing Clojupyter
using Anaconda Cloud.  Key features of Clojupyter's conda support:

* As simple as possible, enough to get started
* 3 platforms are supported: Linux, MacOS and Windows, all in 64-bit variant
* Clojupyter's version number is the basis of the Clojupyter package version number (major, minor, incremental, qualifier)
* Build numbers start at 1
* The Anaconda Cloud channel is `simplect`
* The Clojupyter package can be installed, upgraded and removed using conda
* Only a single Clojupyter kernel can be conda-installed at a time into a given, conda-managed environment (see below)

### Clojupyter kernels in a conda-managed environment

Conda tries to deliver a complete package management solution most of which we believe is not
relevant for Clojupyter users, the focus of Clojupyter's support is leveraging the Anaconda Cloud
for distribution and enabling the user to do `conda install` on the supported platforms - beyond
that the implementation is as limited as possible.  To keep things simple, and to align with the
version-and-buildnum concepts of conda, **we support only a single conda-installed version of
Clojupyter into a conda-managed environment at time**.  However, you are not necessarily precluded
from using multiple conda-installed Clojupyter versions since conda enables you to create *multiple
conda environments*: You should be able to keep multiple versions/builds on your machine using
conda's environment management facilities, see Conda's documentation for [Managing
Environments](https://docs.conda.io/projects/conda/en/latest/user-guide/tasks/manage-environments.html#)
for more details.
