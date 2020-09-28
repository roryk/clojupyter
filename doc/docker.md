# The Docker way

A Docker image  exists to make trying out Clojupyter easier.  To try it:

1. [Install Docker](https://docs.docker.com/engine/installation/) based on your platform.
2. Run `make docker-build` to build a docker image.
3. Run the image with `docker run --publish 8888:8888 clojupyter:<version>`

The first time you start it Docker will pull the Docker image from `hub.docker.com`, which takes time.

Alternatively, you can run `make docker-config` to setup the base environment but without
running `docker build`. You can then make your custom adjustments to the build environment
before calling `make docker-build`.
