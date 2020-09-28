(ns clojupyter.kernel.handle-event
  (:require [clojupyter.kernel.handle-event.comm-msg :as comm-msg]
            [clojupyter.kernel.handle-event.complete :as complete]
            [clojupyter.kernel.handle-event.execute :as execute]
            [clojupyter.kernel.handle-event.history :as history]
            [clojupyter.kernel.handle-event.inspect :as inspect]
            [clojupyter.kernel.handle-event.interrupt :as interrupt]
            [clojupyter.kernel.handle-event.kernel-info :as kernel-info]
            [clojupyter.kernel.handle-event.ops :refer [call-interceptor]]
            [clojupyter.kernel.handle-event.shutdown :as shutdown]
            [clojupyter.messages :as msgs]
            [clojupyter.state :as state]
            [clojupyter.util-actions :as u!]
            [io.simplect.compose.action :refer [action step]]))

(defn- handle-comm
  [ctx]
  (let [S (state/comm-state-get)
        [A S'] (comm-msg/handle-message S ctx)]
    {:leave-action (action A (step [`state/comm-state-swap! (constantly S')]
                                   {:op :comm-state-swap :old-state S :new-state S'}))}))

(defn- handle-execute-request
  [ctx]
  (state/with-current-context [ctx]
    (execute/eval-request ctx)))

(defn- impossible
  [{:keys [req-message] :as ctx}]
  (assert req-message)
  (throw (ex-info (str "handle-event - internal error: " (msgs/message-msg-type req-message))
           {:ctx ctx})))

(defn- unsupported
  [{:keys [req-message] :as ctx}]
  (assert req-message)
  (throw (ex-info (str "handle-event - unhandled-event: " (msgs/message-msg-type req-message))
           {:ctx ctx})))

(defmulti calc
  (fn [msgtype _] msgtype))
(defmethod calc :default [msgtype ctx]
  (throw (ex-info (str "Unhandled message type: " msgtype)
           {:msgtype msgtype, :ctx ctx})))

(defmethod calc msgs/CLEAR-OUTPUT		[_ ctx]	(impossible ctx))
(defmethod calc msgs/COMM-CLOSE			[_ ctx]	(handle-comm ctx))
(defmethod calc msgs/COMM-INFO-REPLY		[_ ctx]	(handle-comm ctx))
(defmethod calc msgs/COMM-INFO-REQUEST		[_ ctx]	(handle-comm ctx))
(defmethod calc msgs/COMM-MSG			[_ ctx]	(handle-comm ctx))
(defmethod calc msgs/COMM-OPEN			[_ ctx]	(handle-comm ctx))
(defmethod calc msgs/COMPLETE-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/COMPLETE-REQUEST		[_ ctx]	(call-interceptor ctx [complete/ic*complete]))
(defmethod calc msgs/ERROR			[_ ctx]	(impossible ctx))
(defmethod calc msgs/EXECUTE-INPUT		[_ ctx]	(impossible ctx))
(defmethod calc msgs/EXECUTE-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/EXECUTE-REQUEST		[_ ctx]	(handle-execute-request ctx))
(defmethod calc msgs/EXECUTE-RESULT		[_ ctx]	(impossible ctx))
(defmethod calc msgs/HISTORY-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/HISTORY-REQUEST		[_ ctx]	(call-interceptor ctx [history/ic*history]))
(defmethod calc msgs/INPUT-REQUEST		[_ ctx]	(impossible ctx))
(defmethod calc msgs/INSPECT-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/INSPECT-REQUEST		[_ ctx]	(call-interceptor ctx [inspect/ic*inspect]))
(defmethod calc msgs/INTERRUPT-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/INTERRUPT-REQUEST		[_ ctx]	(call-interceptor ctx [interrupt/ic*interrupt]))
(defmethod calc msgs/IS-COMPLETE-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/IS-COMPLETE-REQUEST 	[_ ctx]	(call-interceptor ctx [complete/ic*is-complete]))
(defmethod calc msgs/KERNEL-INFO-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/KERNEL-INFO-REQUEST 	[_ ctx]	(call-interceptor ctx [kernel-info/ic*kernel-info]))
(defmethod calc msgs/PROTOCOL-VERSION		[_ ctx]	(unsupported ctx))
(defmethod calc msgs/SHUTDOWN-REPLY		[_ ctx]	(impossible ctx))
(defmethod calc msgs/SHUTDOWN-REQUEST		[_ ctx]	(call-interceptor ctx [shutdown/ic*shutdown]))
(defmethod calc msgs/STATUS			[_ ctx]	(impossible ctx))
(defmethod calc msgs/STREAM			[_ ctx]	(impossible ctx))
(defmethod calc msgs/UPDATE-DISPLAY-DATA 	[_ ctx]	(impossible ctx))

(defn calculate-response
  [{:keys [req-message] :as ctx}]
  (calc (msgs/message-msg-type req-message) ctx))

(defn handle-event!
  "Calculates and executes actions in response to Jupyter request `:req-message` in `ctx`.  Effectful (not pure)."
  [ctx]
  (let [response (calculate-response ctx)]
    (u!/execute-leave-action response)))
