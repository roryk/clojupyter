(ns clojupyter.zmq
  (:require [clojupyter.log :as log]
            [clojupyter.shutdown :as shutdown]
            [clojupyter.util-actions :as u!]
            [clojupyter.zmq-specs :as zp]
            [clojupyter.zmq-util :as zutil]
            [clojure.core.async :as async :refer [>!! alts!! chan]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [clojure.string :as str]
            [io.simplect.compose :refer [C c]]))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; INTERNAL
;;; ------------------------------------------------------------------------------------------------------------------------

(defn channel-forward-start
  [ztx id data-socket-addr fwd-chan terminate! term]
  (let [term-ch (shutdown/notify-on-shutdown term (chan 1))
        fmtmsg (fn [msg] (str "channel-forward(" id ", " data-socket-addr ") -- " msg "."))
        fmtdbg (C fmtmsg #(log/debug %))
        fmterr (C fmtmsg #(log/error %))]
    (zutil/zmq-thread
     (shutdown/initiating-shutdown-on-exit [:channel-forward-start term]
       (u!/with-exception-logging
           (zutil/rebind-context-shadowing [ztx]
             (let [data-sock (doto (zutil/zsocket ztx :pair) (.connect data-socket-addr))]
               (try (fmtdbg "Starting")
                    (loop []
                      (let [[data rcv-chan] (alts!! [term-ch fwd-chan] :priority true)]
                        (cond
                          (shutdown/is-token? data)
                          ,, (fmtdbg "Shutdown message received")
                          (= rcv-chan term-ch)
                          ,, (fmtdbg "Term signal received")
                          (nil? data)
                          ,, (fmtdbg "Outbound channel closed")
                          (and (= rcv-chan fwd-chan) data)
                          ,, (if (zutil/send-frames data-sock data)
                               (recur)
                               (do (fmterr "Could not send all frames - terminated.")
                                   (terminate!)))
                          :else
                          (let [errstr (fmtmsg "Internal error occurred")]
                            (log/error errstr)
                            (throw (ex-info errstr {:data data}))))))
                    (finally
                      (terminate!)
                      (fmtdbg "Terminating"))))))))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; START ZMQ SOCKET FORWARDING
;;; ------------------------------------------------------------------------------------------------------------------------

(defn- inproc-addr
  [prefix suffix]
  (str/lower-case (str "inproc://" prefix "-" suffix)))

(defn- select-bind-or-connect
  [connect?]
  (if connect?
    (fn [sock addr] (.connect sock addr))
    (fn [sock addr] (.bind sock addr))))

(defn start
  ""
  ([ztx id jupyter-socket-addr term]
   (start ztx id jupyter-socket-addr term {}))
  ([ztx id jupyter-socket-addr term
    {:keys [bufsize connect? zmq-socket-type inbound-ch outbound-ch timeout]}]
   (u!/with-exception-logging
       (let [zmq-socket-type (or zmq-socket-type :router)
             inbound-ch (or inbound-ch (chan bufsize))
             outbound-ch (or outbound-ch (chan bufsize))
             timeout (or timeout 250)
             data-socket-addr (inproc-addr (str "fwd-" id) (gensym))
             continue? (atom true)
             terminate! #(reset! continue? false)
             fmtmsg #(str "socket-fwd(" id ") -- " % ".")
             fmtdbg (C fmtmsg #(log/debug %))
             fmtinf (C fmtmsg #(log/info %))
             fmterr (C fmtmsg #(log/error %))]
         (channel-forward-start ztx id data-socket-addr outbound-ch terminate! term)
         (zutil/zmq-thread
          (shutdown/initiating-shutdown-on-exit [:zmq-start term]
            (zutil/rebind-context-shadowing [ztx]
              (try (fmtdbg "Starting")
                   (let [b-or-c (select-bind-or-connect connect?)
                         jsock (doto (zutil/zsocket ztx zmq-socket-type)
                                 (b-or-c jupyter-socket-addr))
                         dsock (doto (zutil/zsocket ztx :pair) (.bind data-socket-addr))
                         poller (.createPoller ztx 2)
                         jpoll (.register poller jsock (zutil/poll-events :pollin :pollerr))
                         dpoll (.register poller dsock (zutil/poll-events :pollin :pollerr))]
                     (u!/closing-channels-on-exit! [outbound-ch inbound-ch]
                       (loop []
                         (let [poll (.poll poller timeout)]
                           (cond
                             (neg? poll)
                             ,, (do (fmtinf "Polling ZeroMQ sockets returned negative value - terminating")
                                    (terminate!))
                             (zero? poll)
                             ,, nil
                             :else
                             (do (when (.pollin poller jpoll)
                                   (when-not (>!! inbound-ch (zutil/receive-frames jsock))
                                     (fmtinf "Inbound channel closed.")
                                     (terminate!)))
                                 (when (.pollin poller dpoll)
                                   (let [frames (zutil/receive-frames dsock)]
                                     (when-not (zutil/send-frames jsock frames)
                                       (fmterr "Could not send all frames - terminating")
                                       (terminate!))))
                                 (when (.pollerr poller jpoll)
                                   (fmterr "Error polling Jupyter ZeroMQ socket - terminating")
                                   (terminate!))
                                 (when (.pollerr poller dpoll)
                                   (fmterr "Error polling outbount ZeroMQ socket - terminating")
                                   (terminate!)))))
                         (when @continue?
                           (recur)))))
                   (catch Exception e
                     (fmterr (str "Exception occurred: " e " (not rethrowing)")))
                   (finally
                     (fmtdbg "Terminating"))))))
         [inbound-ch outbound-ch]))))

(s/fdef start
  :args (s/cat :ztx ::zp/zcontext
               :id (constantly true)
               :zmq-endpoint string?
               :terminator shutdown/terminator?
               :opts (s/? map?))
  :ret (s/and (s/coll-of ::zp/zsocket) ::zp/two-tuple))

(instrument `start)
