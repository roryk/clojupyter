(ns clojupyter
  (:require [clojupyter.display :as display]
            [clojure.java.io :as io])
  (:import javax.imageio.ImageIO))

(def license
  (->> (io/resource "clojupyter/assets/license.txt")
       slurp
       (display/render-mime :text/plain)))

(def logo
  (->> (io/resource "clojupyter/assets/logo-350x80.png")
       (ImageIO/read)))

(def version
  (->> (io/resource "META-INF/MANIFEST.MF")
      slurp
      (re-find  #"Leiningen-Project-Version: (.+)")
      second))
