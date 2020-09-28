(ns clojupyter.shutdown
  "Implements Clojupter for shutting down: All threads monitor a special channel to which a shutdown
  token is sent when Clojupyter is to terminate."
  (:require [clojupyter.log :as log]
            [clojupyter.util-actions :as u!]
            [clojure.core.async :as async]
            [clojure.pprint :as pp]
            [io.simplect.compose :refer [P]]))

(defmacro initiating-shutdown-on-exit
  [[token terminator-binding] & body]
  `(try ~@body
        (finally
          (log/debug (str "Initiating shutdown: " ~token))
          (Thread/sleep 10)
          (initiate ~terminator-binding))))

(u!/set-defn-indent! #'initiating-shutdown-on-exit)

(defprotocol shutdown-proto
  (initiate [terminator]
    "If called starts shutdown of Clojupyter by signalling termination channels.")
  (notify-on-shutdown [terminator ch]
    "Updates `ch` to receive communication sent on the termination channel."))

(defn- fmt [_] "#Terminator")

(defrecord Terminator [shut-ch shut-fn tap-fn]
  shutdown-proto
  (initiate [_] ()
    (shut-fn)
    :initiated)
  (notify-on-shutdown [_ ch]
    (tap-fn ch))
  Object
  (toString [ci] (fmt ci)))

(map (P alter-meta! assoc :private true) [#'->Terminator #'map->Terminator])

(defmethod print-method Terminator
  [^Terminator t w]
  (.write w (fmt t)))

(defmethod pp/simple-dispatch Terminator
  [^Terminator ci]
  (print (fmt ci)))

(def TOKEN :clojupyter/shutdown!)

(defn is-token?
  [v]
  (= v TOKEN))

(defn make-terminator
  [bufsize]
  (let [shut-ch (async/chan bufsize)
        terminate! #(async/>!! shut-ch TOKEN)
        M (async/mult shut-ch)
        tap (fn [ch] (async/tap M ch) ch)]
    (->Terminator shut-ch terminate! tap)))

(defn terminator?
  [v]
  (instance? Terminator v))
