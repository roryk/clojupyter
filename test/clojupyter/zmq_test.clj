(ns clojupyter.zmq-test
  (:require [clojupyter.kernel.core :as core]
            [clojupyter.kernel.jup-channels :as jup]
            [clojupyter.log :as log]
            [clojupyter.messages :as msgs]
            [clojupyter.messages-generators-test :as mg]
            [clojupyter.shutdown :as shutdown]
            [clojupyter.state :as state]
            [clojupyter.test-shared :as ts]
            [clojupyter.test-shared-generators :as shg]
            [clojupyter.util :as u]
            [clojupyter.util-actions :as u!]
            [clojupyter.zmq :as cjpzmq]
            [clojupyter.zmq-util :as zutil]
            [clojure.core.async :as async :refer [<!! >!! chan]]
            [clojure.test.check.generators :as gen :refer [sample]]
            [io.simplect.compose :refer [c C p P]]
            [midje.sweet :refer [=> fact]]))

(state/ensure-initial-state!)

(let [ITERS 50]
  (fact
   "Basic socket-to-channel functionality works"
   (log/with-level :error
     (let [term (shutdown/make-terminator 1)
           addr (str "inproc://chan-sock-test-" (gensym))]
       (zutil/with-shadow-context [ztx (state/zmq-context)]
         (shutdown/initiating-shutdown-on-exit [:test term]
           (let [[inbound-1 outbound-1] (cjpzmq/start ztx :one addr term
                                                      {:zmq-socket-type :pair})
                 [inbound-2 outbound-2] (cjpzmq/start ztx :two addr term
                                                      {:connect? true, :zmq-socket-type :pair})]
             (count (for [k (range ITERS)]
                      (let [v1 [(u/string->bytes (str "msg-" k))]
                            v2 [(u/string->bytes (str "MSG-" k))]]
                        (>!! outbound-1 v1)
                        (assert (= (map (p into []) v1)
                                   (map (p into []) (<!! inbound-2))))
                        (>!! outbound-2 v2)
                        (assert (= (map (p into []) v2)
                                   (map (p into [])  (<!! inbound-1))))))))))))
   => ITERS))

