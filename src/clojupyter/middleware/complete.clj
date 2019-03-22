(ns clojupyter.middleware.complete
  (:require
   [clojure.pprint			:as pp		:refer [pprint]]
   [clojure.spec.alpha			:as s]
   [clojure.string			:as str]
   [net.cgrand.sjacket.parser		:as p]
   [pandect.algo.sha256					:refer [sha256-hmac]]
   [taoensso.timbre			:as log]
   [zeromq.zmq				:as zmq]

   ,,
   [clojupyter.misc.jupyter		:as jup]
   [clojupyter.nrepl.nrepl-comm		:as pnrepl]
   [clojupyter.transport		:as tp		:refer [handler-when transport-layer
                                                                response-mapping-transport
                                                                parent-msgtype-pred]]
   [clojupyter.misc.spec		:as sp]
   [clojupyter.kernel.state		:as state]
   [clojupyter.misc.util		:as u]
   ))

(defn- complete?
  [code]
  (not (some  #(= :net.cgrand.parsley/unfinished %)
              (map :tag (tree-seq :tag
                                  :content
                                  (p/parser code))))))

(def wrap-is-complete-request
  (handler-when (parent-msgtype-pred jup/IS-COMPLETE-REQUEST)
   (fn [{:keys [transport parent-message] :as ctx}]
     (tp/send-req transport jup/IS-COMPLETE-REPLY
       (if (complete? (u/message-code parent-message))
         {:status "complete"}
         {:status "incomplete"})))))

(def wrap-complete-request
  (handler-when (parent-msgtype-pred jup/COMPLETE-REQUEST)
   (fn [{:keys [transport nrepl-comm parent-message] :as ctx}]
     (tp/send-req transport jup/COMPLETE-REPLY
       (let [delimiters #{\( \" \% \space}
             cursor_pos (u/message-cursor-pos parent-message)
             codestr (subs (u/message-code parent-message) 0 cursor_pos)
             sym (as-> (reverse codestr) $
                   (take-while #(not (contains? delimiters %)) $)
                   (apply str (reverse $)))]
         {:matches (pnrepl/nrepl-complete nrepl-comm sym)
          :metadata {:_jupyter_types_experimental []}
          :cursor_start (- cursor_pos (count sym))
          :cursor_end cursor_pos
          :status "ok"})))))
