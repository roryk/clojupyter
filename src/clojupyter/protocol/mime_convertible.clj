(ns clojupyter.protocol.mime-convertible
  (:require [clojupyter.util :as u]))


(defprotocol PMimeConvertible
  (to-mime [o]))

(def stream-to-string
  "Returns JSON representation (string) of `v`. DEPRECATED - use `to-json-str` instead."
  u/to-json-str)
