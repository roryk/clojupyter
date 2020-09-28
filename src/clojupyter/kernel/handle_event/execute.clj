(ns clojupyter.kernel.handle-event.execute
  (:require [clojupyter.kernel.cljsrv :as cljsrv]
            [clojupyter.kernel.handle-event.ops :as ops
             :refer [definterceptor s*append-enter-action s*append-leave-action]]
            [clojupyter.kernel.jup-channels :refer [receive!! send!!]]
            [clojupyter.log :as log]
            [clojupyter.messages :as msgs]
            [clojupyter.plan :as pl :refer [s*bind-state s*when s*when-not]]
            [clojupyter.state :as state]
            [clojupyter.util :as u]
            [clojure.spec.alpha :as s]
            [clojure.set :as set]
            [io.pedestal.interceptor.chain :as ich]
            [io.simplect.compose :refer [def- C p P]]
            [io.simplect.compose.action :as a :refer [action step]]))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; STATE OPS
;;; ------------------------------------------------------------------------------------------------------------------------

(defn- s*append-output		[stream s]	(pl/s*append-value :output {:stream stream :string s}))

(defn- s*update-interrupted	[f]		(pl/s*update-value :interrupted? f))
(def-  s*set-interrupted!			(s*update-interrupted (constantly true)))
(def-  s*clear-interrupted!			(s*update-interrupted (constantly false)))
(def-  interrupted?				(pl/get-value :interrupted? false))

(defn- s*update-ns		[f]		(pl/s*update-value :ns f))
(defn- s*set-ns			[v]		(s*update-ns (constantly v)))
(def-  get-ns					(pl/get-value :ns))

(defn- s*set-need-stacktrace	[v]		(pl/s*set-value :need-stacktrace? (boolean v)))
(def-  need-stacktrace?				(pl/get-value :need-stacktrace?))

(defn- s*update-result		[f]		(pl/s*update-value :result (fnil f {})))
(defn- s*set-result		[v]		(s*update-result (constantly v)))
(def-  get-result				(pl/get-value :result))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; STACKTRACE
;;; ------------------------------------------------------------------------------------------------------------------------

(defn collect-stacktrace-strings
  "Return a nicely formatted string."
  [{:keys [stacktrace]}]
  (when stacktrace
    (let [skip-tags	#{"dup" "tooling" "repl"}
          relevant	(filter (C :flags
                                   (p into #{})
                                   (p set/intersection skip-tags)
                                   (p = #{}))
                                stacktrace)
          maxlen	(fn [k] (reduce max 1 (map (C k count) relevant)))
          format-str	(str "%" (maxlen :file) "s: %5d %-" (maxlen :file) "s")]
      (vec (for [{:keys [file line name]} relevant]
             (format format-str file line name))))))

;;; ----------------------------------------------------------------------------------------------------
;;; NREPL EVAL
;;; ----------------------------------------------------------------------------------------------------

(defn s*interpret-nrepl-message
  [{:keys [ns out err ex mime-tagged-value status]}]
  (C (s*when ns
       (s*set-ns ns))
     (s*when out
       (s*append-output "stdout" out))
     (s*when err
       (s*append-output "stderr" err))
     (s*when ex
       (s*update-result (P assoc :ename ex)))
     (s*when mime-tagged-value
       (s*update-result (P assoc :result mime-tagged-value)))
     (s*when (some #{"interrupted"} status)
       (C (s*update-result (P assoc :ename "interrupted"))
          s*set-interrupted!))))

(defn s*interpret-nrepl-eval-results
  [nrepl-messages]
  (fn [S] (reduce (fn [Σ m] ((s*interpret-nrepl-message m) Σ)) S nrepl-messages)))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; EXECUTE INTERCEPTOR
;;; ------------------------------------------------------------------------------------------------------------------------

(def- s*a-l s*append-leave-action)

(defn- get-input!
  [{:keys [jup req-message]}]
  ;; send INPUT-REQUEST on `iopub_port`:
  (send!! jup :stdin_port req-message msgs/INPUT-REQUEST (msgs/input-request-content "Enter value: "))
  ;; wait for INPUT-REPLY:
  (let [{received-message :req-message :as received} (receive!! jup :stdin_port)
        msgvalue  (msgs/message-value received-message)]
    msgvalue))

(defn- silent-eval?
  [{:keys [req-message]}]
  (or (msgs/message-silent req-message) (-> req-message msgs/message-code u/code-empty?)))

(defn- handle-eval-result
  [{:keys [jup req-port cljsrv req-message nrepl-eval-result] :as ctx} continuing?]
  (assert req-port)
  (assert cljsrv)
  (assert req-message)
  (assert nrepl-eval-result)
  (let [{:keys [nrepl-messages
                need-input
                delayed-msgseq
                trace-result]}	nrepl-eval-result
        _			(when need-input
                                  (assert delayed-msgseq))
        code 			(msgs/message-code req-message)
        exe-count		(state/execute-count)
        eval-interpretation	((s*interpret-nrepl-eval-results nrepl-messages) {})
        {:keys [interrupted?
                result output]}	eval-interpretation
        {:keys [ename]}		result
        silent?			(silent-eval? ctx)
        hushed?			(u/code-hushed? code)
        store-history?		(if silent? false (msgs/message-store-history? req-message))
        halting?		(or interrupted? ename)
        first-segment?		(not continuing?)
        final-segment?		(or halting? (not need-input))
        reply			(if ename
                                  (msgs/execute-reply-content "error" exe-count
                                                              {:traceback (collect-stacktrace-strings trace-result),
                                                               :ename ename})
                                  (msgs/execute-reply-content "ok" exe-count))
        nrepl-ctx		(state/current-context)
        nrepl-leave-action	(:leave-action nrepl-ctx)
        send-step		(fn [sock-kw msgtype message]
                                  (step (fn [S] (send!! jup sock-kw req-message msgtype message) S)
                                        {:message-to sock-kw :msgtype msgtype :message message}))]
    (C (s*a-l (step identity
                    {:op :no-op
                     :interpretation {:interpretation eval-interpretation,
                                      :ename ename, :silent? silent?, :hushed? hushed?,
                                      :store-history? store-history?, :reply reply
                                      :interrupted? interrupted?, :need-input need-input,
                                      :halting? halting?, :final-segment? final-segment?,
                                      :nrepl-messages nrepl-messages}}))
       (s*when nrepl-leave-action
         (s*a-l nrepl-leave-action))
       (s*when interrupted?
         (s*a-l (send-step :iopub_port msgs/STREAM (msgs/stream-message-content "stderr" "*Interrupted*\n"))))
       (s*when output
         (s*a-l (apply action
                       (doall ;; strict evaluation is necessary here
                        (for [{:keys [stream string]} output]
                          (send-step :iopub_port msgs/STREAM (msgs/stream-message-content stream string)))))))
       (s*when (and need-input (not halting?))
         (s*a-l (step #(assoc % :user-input (get-input! ctx))
                      {:op :get-input})))
       (s*when (and final-segment? (not silent?))
         (if ename
           (s*a-l (send-step :iopub_port msgs/ERROR reply))
           (s*when-not hushed?
             (s*a-l (send-step :iopub_port msgs/EXECUTE-RESULT
                               (msgs/execute-result-content (u/parse-json-str (:result result) true) exe-count))))))
       (s*when final-segment?
         (if ename
           (s*a-l (send-step req-port msgs/ERROR reply))
           (s*a-l (send-step req-port msgs/EXECUTE-REPLY reply))))
       (s*when (and store-history? final-segment?)
         (s*a-l (step [`state/add-history! code]
                      {:op :add-history, :data code})))
       (s*when (and (not silent?) final-segment?)
         (s*a-l (step [`state/inc-execute-count!]
                      {:op :inc-execute-count}))))))

(definterceptor ic*eval-code msgs/EXECUTE-REQUEST
  (s*bind-state {:keys [jup req-message cljsrv] :as ctx}
    (do (assert req-message)
        (assert cljsrv)
        (let [silent? (silent-eval? ctx)
              exe-count (state/execute-count)
              code (msgs/message-code req-message)
              send-step (fn [sock-kw msgtype message]
                          (step [`send!! jup sock-kw req-message msgtype message]
                                {:message-to sock-kw :msgtype msgtype :message message}))]
          (C (s*when-not silent?
               (-> (send-step :iopub_port msgs/EXECUTE-INPUT (msgs/execute-input-msg-content exe-count code))
                   s*append-enter-action))
             (-> (step #(assoc % :nrepl-eval-result (cljsrv/nrepl-eval cljsrv code))
                       {:op :nrepl-eval :code code})
                 s*append-enter-action)))))
  (s*bind-state ctx
    (handle-eval-result ctx false)))

(definterceptor ic*provide-input msgs/EXECUTE-REQUEST
  (s*bind-state {:keys [req-message cljsrv user-input delayed-msgseq] :as ctx}
    (do (log/debug "ic*provide-input" (log/ppstr {:ctx ctx}))
        (assert req-message)
        (assert cljsrv)
        (assert (string? user-input))
        (assert (delay? delayed-msgseq))
        (s*append-enter-action
         (action (step [`cljsrv/nrepl-provide-input cljsrv user-input]
                       {:op :provide-input :user-input user-input})
                 (step #(assoc % :nrepl-eval-result (cljsrv/nrepl-continue-eval cljsrv @delayed-msgseq))
                       {:op :continue-eval})))))
  (s*bind-state ctx
    (handle-eval-result ctx true)))

(defn eval-request
  ([ctx]
   (eval-request ctx [ic*eval-code]))
  ([ctx interceptors]
   (loop [{:keys [nrepl-eval-result] :as ctx'}
          ,, (ich/execute (-> ctx
                              (ops/set-enter-action (action nil)))
                          (conj interceptors ops/enter-action-interceptor))]
     (let [{:keys [need-input delayed-msgseq]} nrepl-eval-result]
       (if need-input
         (let [ctx'' ((ops/invoke-action ops/get-leave-action) ctx')]
           (recur (ich/execute (-> ctx''
                                   (update :leave-actions (fnil conj []) (ops/get-leave-action ctx''))
                                   (dissoc :need-input)
                                   (assoc :delayed-msgseq delayed-msgseq)
                                   (ops/set-enter-action (action nil))
                                   (ops/set-leave-action (action nil)))
                               [ic*provide-input ops/enter-action-interceptor])))
         ctx')))))
