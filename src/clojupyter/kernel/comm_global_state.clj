(ns clojupyter.kernel.comm-global-state
  "Implements functionality needed to manipulate COMM object state(s)."
  (:require
   [clojupyter.util-actions :as u!]))

(declare ->CommGlobalState)

(defn- mkstate
  ([]
   (mkstate {}))
  ([comms]
   (->CommGlobalState comms)))

(defprotocol comm-global-state-proto
  ;; You could argue that a protocol for this is overkill
  (comm-atom-get [comm-state comm-id] [comm-state comm-id default])
  (comm-atom-remove [comm-state comm-id])
  (comm-atom-add [comm-state comm-id comm])
  (known-comm-ids [comm-state])
  (known-comm-id? [comm-state comm-id]))

(defrecord CommGlobalState [comms_]
  comm-global-state-proto
  (comm-atom-get [comm-state comm-id]
    (comm-atom-get comm-state  comm-id nil))
  (comm-atom-get [_ comm-id default]
    (get comms_ comm-id default))
  (comm-atom-remove [_ comm-id]
    (mkstate (dissoc comms_ comm-id)))
  (comm-atom-add [_ comm-id comm]
    (mkstate (assoc comms_ comm-id comm)))
  (known-comm-ids [_]
    (keys comms_))
  (known-comm-id? [comm-state comm-id]
    (boolean (comm-atom-get comm-state comm-id))))

(map u!/set-var-private! [#'->CommGlobalState #'map->CommGlobalState])

(def comm-state? (partial instance? CommGlobalState))

(defn initial-state
  []
  (mkstate))
