(ns clojupyter.display
  (:require [clojupyter.protocol.mime-convertible :as mc]
            [clojupyter.util :as u]
            [clojure.data.codec.base64 :as b64]
            [hiccup.core :as hiccup]))

(defn render-mime
  [mime-type v]
  (reify mc/PMimeConvertible
    (to-mime [_]
      (u/to-json-str (hash-map mime-type v)))))

;; HTML

(defn hiccup
  [v]
  (render-mime :text/html (hiccup/html v)))

(defn html
  [v]
  (render-mime :text/html v))

;; Latex

(defn latex
  [v]
  (render-mime :text/latex v))

;; Markdown

(defn markdown
  [v]
  (render-mime :text/markdown v))


;; Vega Lite

(defn vega-lite-1
  [v]
  (render-mime :application/vnd.vegalite.v1+json v))

(defn vega-lite-3
  [v]
  (render-mime :application/vnd.vegalite.v3+json v))

(defn vega-lite
  [v]
  (vega-lite-3 v))

;; Vega

(defn vega-5
  [v]
  (render-mime :application/vnd.vega.v5+json v))

(defn vega
  [v]
  (vega-5 v))

;; Gif

(defn gif
  [b]
  (render-mime :image/gif (new String (b64/encode b))))

;; Pdf

(defn pdf
  [b]
  (render-mime :application/pdf (new String (b64/encode b))))

;; JSON

(defn json
  [v]
  (render-mime :application/json v))

;; VDOM

(defn vdom
  [v]
  (render-mime :application/vdom.v1+json v))
