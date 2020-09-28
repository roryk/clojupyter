.PHONY: clean metajar install build docker-build docker-config conda-build conda-config docker-clean

VERSION = $(shell bin/version)
IDENT = clojupyter-$(VERSION)
docker-files = docker/Dockerfile docker/kernel/kernel.json docker/kernel/logo-64x64.png docker/lib/$(IDENT).jar
clojure-files = $(shell find src -iname '*.clj')
resource-files = $(shell find resources -type f)
conda-files = conda/meta.yaml conda/bld.bat conda/build.sh

build: target/$(IDENT).jar

docker-config: $(docker-files)

conda-config: build target/logo-64x64.png $(conda-files)

clean: docker-clean
	lein clean


install: build bin/install
	bin/install

docker-build: docker-config
	docker build --build-arg IDENT=$(IDENT) -t clojupyter:$(VERSION) docker

target/$(IDENT).jar: $(clojure-files) $(resource-files) project.clj
	lein metajar

target/logo-64x64.png: resources/clojupyter/assets/logo-64x64.png
	cp resources/clojupyter/assets/logo-64x64.png target/.


docker/kernel:
	mkdir docker/kernel

docker/lib:
	mkdir docker/lib

docker/kernel/kernel.json: bin/kernel.json project.clj | docker/kernel
	bin/kernel.json /home/jovyan/.local/lib >docker/kernel/kernel.json

docker/kernel/logo-64x64.png: resources/clojupyter/assets/logo-64x64.png | docker/kernel
	cp resources/clojupyter/assets/logo-64x64.png docker/kernel

docker/lib/$(IDENT).jar: target/$(IDENT).jar | docker/lib
	cp -r target/$(IDENT).jar target/lib docker/lib

docker-clean:
	rm -rvf docker/*/


conda/meta.yaml: project.clj $(clojure-files)
ifeq ($(VERSION),$(shell sed -nE 's/^[[:space:]]*version: (.+)$$/\1/g p' conda/meta.yaml))
	buildno=$$(sed -nE 's/^[[:space:]]*number: ([[:digit:]]+)$$/\1/g p' conda/meta.yaml) && \
	sed -i -E "s/^([[:space:]]*number: )[[:digit:]]+$$/\1$$((++buildno))/g" conda/meta.yaml
else
	sed -i -E -e "s/^([[:space:]]*number: )[[:digit:]]+$$/\10/g" \
	       -e "s/^([[:space:]]*version: ).+$$/\1$(VERSION)/g" conda/meta.yaml
endif

conda-build: conda-config
	conda build conda
