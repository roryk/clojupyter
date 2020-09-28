FROM jupyter/minimal-notebook
LABEL maintainer="Klaus Harbo <klaus@harbo.net>"

ARG NB_USER=jovyan

ENV NOTEBOOK_PATH $HOME/notebooks
ENV PORT 8888
ARG IDENT
ENV CJP_LIB_PATH $HOME/.local/lib/$IDENT
ENV CJP_DATA_PATH $HOME/.local/share/jupyter/kernels/$IDENT

USER root
RUN apt update &&\
    apt-get install -y openjdk-8-jre-headless

USER $NB_USER
RUN mkdir -p $NOTEBOOK_PATH $CJP_LIB_PATH/lib $CJP_DATA_PATH
COPY	lib/$IDENT.jar $CJP_LIB_PATH
COPY	lib/lib $CJP_LIB_PATH/lib
COPY	kernel/logo-64x64.png $CJP_DATA_PATH
COPY	kernel/kernel.json $CJP_DATA_PATH

WORKDIR $NOTEBOOK_PATH
EXPOSE $PORT
VOLUME $NOTEBOOK_PATH
CMD ["jupyter", "notebook", "--log-level=ERROR"]
