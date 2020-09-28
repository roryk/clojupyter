(ns clojupyter.test-shared
  (:require [clojupyter.messages :as msgs]
            [clojupyter.util :as u]
            [clojupyter.util-actions :as u!]
            [io.simplect.compose :refer [C p P]]
            [midje.sweet :as midje]))

(defn s*message-header
  ([msgtype] (s*message-header msgtype {}))
  ([msgtype {:keys [date envelope message-id parent-header session signature username version]}]
   (let [date (or date ""),
         envelope (or envelope [])
         message-id (or message-id "")
         parent-header (or parent-header {})
         session (or session "")
         signature (or signature (u/string->bytes ""))
         username (or username "")
         version (or version msgs/PROTOCOL-VERSION)
         metadata {}
         buffers []
         header (msgs/make-jupmsg-header message-id msgtype username session date version)]
     (fn [content]
       (msgs/make-jupmsg envelope signature header parent-header metadata content buffers)))))

(defn default-execute-request-content
  ([] (default-execute-request-content {}))
  ([{:keys [allow-stdin? code silent stop-on-error? store-history?]}]
   (let [allow-stdin? (or allow-stdin? true)
         code (or code "(do)")
         silent? (boolean (or silent false))
         stop-on-error? (boolean (or stop-on-error? true))
         store-history? (boolean (or store-history? true))]
     (msgs/execute-request-content code allow-stdin? silent? stop-on-error? store-history?))))
