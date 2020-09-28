(defproject clojupyter "0.4.0"
  :description "A Jupyter kernel for Clojure"
  :license     {:name "MIT"}
  :url         "https://github.com/clojupyter/clojupyter"
  :scm         {:name "git" :url "https://github.com/clojupyter/clojupyter"}
  :source-paths      ["src"]
  :resource-paths    ["resources"]

  :dependencies      [[camel-snake-kebab "0.4.1"]
                      [cheshire "5.10.0"]
                      [cider/cider-nrepl "0.25.3"]
                      [clojure.java-time "0.3.2"]
                      [com.cemerick/pomegranate "1.1.0"]
                      [com.grammarly/omniconf "0.4.2"]
                      [com.taoensso/timbre "4.10.0"]
                      [hiccup "1.0.5"]
                      [io.aviso/pretty "0.1.37"]
                      [io.pedestal/pedestal.interceptor "0.5.8"]
                      [io.simplect/compose "0.7.27"]
                      [net.cgrand/parsley "0.9.3" :exclusions [org.clojure/clojure]]
                      [net.cgrand/regex "1.1.0" :exclusions [org.clojure/clojure]]
                      [org.clojars.trptcolin/sjacket "0.1.1.1" :exclusions [org.clojure/clojure net.cgrand.parsley]]
                      [nrepl "0.8.1"]
                      [org.clojure/clojure "1.10.1"]
                      [org.clojure/data.codec "0.1.1"]
                      [org.clojure/java.jdbc "0.7.11"]
                      [org.xerial/sqlite-jdbc "3.32.3.2"]
                      [org.zeromq/jeromq "0.5.2"]
                      [pandect "0.6.1"]
                      [slingshot "0.12.2"]
                      [zprint "1.0.0"]
                      [org.clojure/core.async "1.3.610"]
                      [com.fzakaria/slf4j-timbre "0.3.19"]
                      [org.slf4j/log4j-over-slf4j "1.7.30"]
                      [org.slf4j/jul-to-slf4j "1.7.30"]
                      [org.slf4j/jcl-over-slf4j "1.7.30"]]

  :profiles    {:dev     {:dependencies [[midje "1.9.9" :exclusions [org.clojure/clojure]]
                                         [org.clojure/test.check "1.1.0"]]
                          :plugins      [[lein-midje "3.2.2"]
                                         [lein-metajar "0.1.1"]]}

                :metajar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :aot         [clojupyter.kernel.core]
  :main        clojupyter.kernel.core)
