(ns clojupyter.jupmsg-specs
  (:require
   [clojure.spec.alpha :as s]))


(s/def ::header				(s/keys :req-un [::msg_id ::msg_type ::session]
                                                :opt-un [ ::username ::date ::version]))
(s/def ::parent-header			(s/or :header ::header :empty #(= % {})))
(s/def ::metadata			map?)
(s/def ::content			map?)
(s/def ::jupmsg				(s/keys :req-un [::header ::parent-header
                                                         ::metadata ::content
                                                         ::preframes ::buffers]))
