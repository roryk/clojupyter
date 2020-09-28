(ns clojupyter.kernel.handle-event-process
  (:require [clojupyter.kernel.handle-event :as he]
            [clojupyter.kernel.jup-channels :refer [jup-channels send!!]]
            [clojupyter.messages :as msgs]
            [clojupyter.log :as log]
            [clojupyter.shutdown :as shutdown]
            [clojupyter.util-actions :as u!]
            [clojure.core.async :as async]
            [io.simplect.compose :refer [p]]))

(defn- handle-error
  [msg]
  (log/error "handle-event-process: error on input" (log/ppstr {:msg msg})))

(defn- handle-event-or-channel-error
  "Handles request, returns `false` iff event-handling should terminate."
  [{:keys [jup] :as ctx} channel-port port-msgtype-pred inbound-msg]
  (let [{:keys [req-message req-port error?]} inbound-msg
        update-status! #(send!! jup :iopub_port req-message msgs/STATUS (msgs/status-message-content %))
        msgtype (msgs/message-msg-type req-message)
        valid? (port-msgtype-pred msgtype)]
    (assert (= channel-port req-port)
            (str "handle-request - internal error ('" channel-port "' neq '"  req-port "')"))
    (cond
      error?
      ,, (do (handle-error inbound-msg)
             ;; continue after error:
             true)
      (not valid?)
      ,, (throw (ex-info (str "Invalid msgtype for port " channel-port ": " msgtype)
                  {:req-message req-message, :msgtype msgtype, :channel-port channel-port}))
      :else
      ,, (do (update-status! "busy")
             (he/handle-event! (assoc ctx :req-message req-message :req-port req-port))
             (update-status! "idle")
             ;; continue if we are not handling a shutdown request:
             (not= (msgs/message-msg-type req-message) msgs/SHUTDOWN-REQUEST)))))

(defn- run-dispatch-loop
  [sock-kw port-msgtype-pred {:keys [jup term] :as ctx}]
  (let [term-ch (shutdown/notify-on-shutdown term (async/chan 1))
        [in-ch _] (jup-channels jup sock-kw)]
    (loop []
      (let [[inmsg-or-token _] (async/alts!! [term-ch in-ch] :priority true)]
        (cond
          (nil? inmsg-or-token)
          ,, (log/debug (str "handle-event-process received nil - terminating: " sock-kw))
          (shutdown/is-token? inmsg-or-token)
          ,, (log/debug (str "handle-event-process received shutdown - terminating: " sock-kw))
          :else
          ,, (when (handle-event-or-channel-error ctx sock-kw port-msgtype-pred inmsg-or-token)
               (recur)))))))

(defn start-channel-thread
  [sock-kw valid-msgtype-pred {:keys [term] :as ctx}]
  (async/thread
    (shutdown/initiating-shutdown-on-exit [:handle-event-process term]
      (u!/with-exception-logging
          (do (log/debug (str "handle-event-process starting: " sock-kw))
              (run-dispatch-loop sock-kw valid-msgtype-pred ctx))
        (log/debug (str "handle-event-process terminating: "  sock-kw))))))

(def valid-control-port-msgtype? (p contains? #{msgs/SHUTDOWN-REQUEST msgs/INTERRUPT-REQUEST}))

(defn start-handle-event-process
  [ctx]
  (doseq [[sock-kw pred] [[:control_port 	valid-control-port-msgtype?]
                          [:shell_port		(complement valid-control-port-msgtype?)]]]
    (start-channel-thread sock-kw pred ctx)))
