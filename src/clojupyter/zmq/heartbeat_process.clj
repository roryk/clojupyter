(ns clojupyter.zmq.heartbeat-process
  (:require [clojupyter.log :as log]
            [clojupyter.shutdown :as shutdown]
            [clojupyter.util-actions :as u!]
            [clojupyter.zmq-util :as zutil]
            [clojure.core.async :as async :refer [>!! chan]]
            [io.simplect.compose :refer [C]]))

(defn start-hb
  ([ztx hb-socket-addr term]
   (start-hb ztx hb-socket-addr term {}))
  ([ztx hb-socket-addr term {:keys [timeout
                                    ;; term-signal-ch is exclusively used for testing,
                                    ;; to verify hb termination:
                                    term-signal-ch]}]
   (let [term-ch (shutdown/notify-on-shutdown term (async/chan 1))
         timeout (or timeout 200)
         fmtmsg #(str "heartbeat: " % ".")
         fmtdbg (C fmtmsg #(log/debug %))
         fmterr (C fmtmsg #(log/error %))]
     (zutil/zmq-thread
      (shutdown/initiating-shutdown-on-exit [:hb term]
        (fmtdbg "Starting")
        (u!/with-exception-logging
            (u!/closing-channels-on-exit! [term-ch]
              (try (zutil/rebind-context-shadowing [ztx]
                     (let [continue? (atom true)
                           terminate! #(reset! continue? false)
                           terminate-with (fn [f] (terminate!) (f))
                           socket (doto (zutil/zsocket ztx :rep) (.bind hb-socket-addr))
                           poller (.createPoller ztx 1)
                           p (.register poller socket (zutil/poll-events :pollin :pollerr))]
                       (loop []
                         (let [poll (.poll poller timeout)]
                           (if (neg? poll)
                             (terminate-with #(fmterr "Polling ZeroMQ socket returned negative value - terminating"))
                             (do (when (.pollin poller p)
                                   ;; heartbeat arrived
                                   (.send socket (.recv socket)))
                                 (when (.pollerr poller p)
                                   ;; error on socket
                                   (terminate-with #(fmterr "Error on ZeroMQ heartbeat socket - terminating")))
                                 (when-let [sign (async/poll! term-ch)]
                                   ;; term signal arrived
                                   (terminate-with #(do (fmtdbg (str "Received term signal (" sign ") - terminating"))
                                                        (Thread/sleep 10))))))
                           (when @continue?
                             (recur))))))
                   (finally
                     (fmtdbg "Terminating")
                     (when term-signal-ch
                       (async/>!! term-signal-ch :hb-terminating)))))))))))
