(ns clojupyter.misc.helper
  (:require [cemerick.pomegranate :as pg]
            [clojupyter.display :as display]))

(defn ^:deprecated add-javascript
  "add a single javascript to front-end, must be last form of cell input

  Example:
    (add-javascript \"https://cdnjs.cloudflare.com/ajax/libs/d3/4.12.2/d3.js\")

  DEPRECATED. Jupyter no longer interprets `script` tags sent from
  kernels and instead uses RequireJS to manage Javascript libraries.
  See elsewhere for details about clojupyter's support for loading
  third party Javascript libraries."
  [script-src]
  (display/hiccup [:script {:src script-src}]))

(defn add-dependencies
  "Use Pomegranate to add dependencies with Maven Central and Clojars as
  default repositories."
  [dependencies & {:keys [repositories]
                   :or {repositories {"central" "https://repo1.maven.org/maven2/"
                                      "clojars" "https://clojars.org/repo"}}}]

  (pg/add-dependencies :coordinates `[~dependencies]
                       :repositories repositories))
