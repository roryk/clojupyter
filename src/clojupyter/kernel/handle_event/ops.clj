(ns clojupyter.kernel.handle-event.ops
  (:require [clojupyter.kernel.jup-channels :refer [send!!]]
            [clojupyter.log :as log]
            [clojupyter.messages :as msgs]
            [io.aviso.exception	:as aviso-ex]
            [io.pedestal.interceptor :as ic]
            [io.pedestal.interceptor.chain :as ich]
            [io.simplect.compose :refer [p P]]
            [io.simplect.compose.action :as a :refer [action failure step]]))

(use 'clojure.pprint)

(defn- append-action		[ctx k a]	(update-in ctx [k] (P action a)))
(defn- get-action		[ctx k]		(get ctx k (action nil)))
(defn- set-action		[ctx k a]	(assoc ctx k a))

(defn- append-enter-action	[ctx a]		(append-action ctx :enter-action a))
(defn- append-leave-action	[ctx a]		(append-action ctx :leave-action a))
(defn get-enter-action		[ctx]		(get-action ctx :enter-action))
(defn set-enter-action		[ctx a]		(set-action ctx :enter-action a))
(defn get-leave-action		[ctx]		(get-action ctx :leave-action))
(defn set-leave-action		[ctx a]		(set-action ctx :leave-action a))

(defn s*set-response
  [msgtype message]
  (fn [{:keys [jup req-message req-port] :as ctx}]
    (assert req-port "s*set-response: req-port not found")
    (set-leave-action ctx (action (step (fn [S]
                                          (send!! jup req-port req-message msgtype message)
                                          S)
                                        {:message-to req-port :msgtype msgtype :message message})))))

(defn s*append-enter-action
  [a]
  (fn [ctx] (append-enter-action ctx a)))

(defn s*append-leave-action
  [a]
  (fn [ctx] (append-leave-action ctx a)))

(defn call-if-msgtype
  [f msgtype {:keys [req-message] :as ctx}]
  (if (= msgtype (msgs/message-msg-type req-message))
    (f ctx)
    ctx))

(defmacro definterceptor
  [nm msgtype enter-fn leave-fn]
  (let [enter (-> nm (str "-enter") symbol)
        leave (-> nm (str "-leave") symbol)]
    `(do
       (def ~enter ~enter-fn)
       (def ~leave ~leave-fn)
       (def ~nm (ic/interceptor
                 {:handles-message ~(-> msgtype resolve var-get)
                  :name ~(->> nm name (keyword (-> *ns* ns-name name)))
                  :enter (p call-if-msgtype ~enter ~msgtype)
                  :leave (p call-if-msgtype ~leave ~msgtype)})))))

(defn invoke-action
  [get-action-fn]
  (fn [ctx]
    (let [action (get-action-fn ctx)
          evaluated-action (action)]
      (if (a/success? evaluated-action)
        (-> (set-enter-action ctx evaluated-action)
            (merge (a/output evaluated-action)))
        (let [fl (failure evaluated-action)
              trace (when fl (binding [aviso-ex/*fonts* nil] (aviso-ex/format-exception fl)))
              logdata {:evaluated-action evaluated-action}]
          (log/error "Action failed:" (log/ppstr logdata)
                     \newline "  Stacktrace:" \newline trace)
          (throw (ex-info (str "Action failed: " evaluated-action) logdata)))))))

(def enter-action-interceptor
  (ic/interceptor
   {:name ::enter-action-interceptor
    :enter (invoke-action get-enter-action)}))

(def leave-action-interceptor
  (ic/interceptor
   {:name ::leave-action-interceptor
    :leave (invoke-action get-leave-action)}))

(defn call-interceptor
  ([input interceptors]
   (call-interceptor input interceptors enter-action-interceptor))
  ([input interceptors response-interceptor]
   (ich/execute input (conj interceptors response-interceptor))))
