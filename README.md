<img src="./resources/clojupyter/assets/logo-350x80.png">

A Jupyter kernel for Clojure - run Clojure code in Jupyter Lab, Notebook and Console.

[![Build Status](https://travis-ci.org/clojupyter/clojupyter.svg?branch=master)](https://travis-ci.org/clojupyter/clojupyter)
[![Clojars Project](https://img.shields.io/clojars/v/clojupyter.svg)](https://clojars.org/clojupyter)

## Table of Contents

* [Getting Started](#getting-started)
* [Installation](#installation)
  * [Usage Scenarios - check here to see options for using Clojupyter](./doc/usage-scenarios.md)
  * [Using Clojupyter Plugins](./doc/plugins.md)
  * [Clojupyter and Conda](./doc/clojupyter-and-conda.md)
  * [Conda-installing Clojupyter](./doc/conda-installing.md)
  * [Docker Image](./doc/docker.md)
* [To do](#todo)
* [Collaboration](#collaboration)

![clojupyter](./images/demo.png)

## Getting Started

In the `examples` folder of the repository are there 3 example notebooks showing some of the
features of clojupyter.  See [this
notebook](https://github.com/clojupyter/clojupyter/blob/master/examples/demo-clojupyter.ipynb)
showing examples of how you can display HTML and use external Javascript:

<img src="./images/html-demo.png" width="100%"/>

There are 3 example notebooks because Jupyter offers several distinct
user interfaces - Jupyter Lab, Jupyter Notebook and Jupyter Console -
which have different feature sets for which clojupyter offers
different support. We have one example notebook showing the features
shared by Lab and Notebook and for each showing their distinct
features. According to the Jupyter development roadmaps, Jupyter
Notebook will eventually be phased out and completely replaced by
Jupyter Lab.

You can also use existing JVM charting libraries since you can render any Java BufferedImage.

<img src="./images/incanter-demo.png" width="100%"/>

## Installation

Clojupyter can be used in several ways, please read [Usage Scenarios](doc/usage-scenarios.md) to
find out which type of use model best fits you needs, and how to install Clojupyter in that
scenario.

## Running Jupyter with Clojupyter


#### Jupyter Notebook

To start Jupyter Notebook do:

```bash
jupyter notebook
```

and choose 'New' in the top right corner and select 'Clojure (clojupyter...)' kernel.

#### Jupyter Lab

To start Jupyter Lab do:

```bash
jupyter lab
```

#### Jupyter Console

You can also start the Jupyter Console by:

```bash
jupyter-console --kernel=<clojupyter-kernel-name>
```
Use `jupyter-kernelspec list` to list all available kernels. So e.g. in case of installing clojupyter using conda the start command is:
```bash
jupyter-console --kernel=conda-clojupyter
```

## TODO

Development progress is based on voluntary efforts so we can't make any promises, but the
wish list for clojupyter development looks something like this:

* [ ] Front-end: Support reindentation, Parinfer, syntax highlighting in code blocks
* [ ] Connect running kernel to running Clojure instances
* [ ] Clarify/simplify external access to rendering - eliminate dependency from Oz to clojupyter
* [X] Support interactive Jupyter Widgets

Feed-back on development priorities is welcome, use the [issue
list](https://github.com/clojupyter/clojupyter/issues) for input and suggestions.


## Collaboration
If you submit a pull request that ends up getting merged, we will give you commit access.
