(ns clojupyter.specs
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::byte-array		#(= (type %) (Class/forName "[B")))
(s/def ::byte-arrays		(s/coll-of ::byte-array :kind vector?))

(s/def ::ctx			(s/keys :req-un [::cljsrv ::jup ::term ::req-message]))
