(ns clojupyter.kernel.handle-event.execute-test
  (:require [clojupyter.kernel.cljsrv :as srv]
            [clojupyter.kernel.core-test :as core-test]
            [clojupyter.kernel.handle-event.execute :as execute]
            [clojupyter.kernel.handle-event.shared-ops :as sh]
            [clojupyter.kernel.init :as init]
            [clojupyter.kernel.jup-channels :as jup]
            [clojupyter.log :as log]
            [clojupyter.messages :as msgs]
            [clojupyter.messages-specs :as msp]
            [clojupyter.test-shared :as ts]
            [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [io.simplect.compose :refer [def- c C p P ]]
            [io.simplect.compose.action :as a]
            [midje.sweet :as midje :refer [=> fact]]
            [nrepl.core :as nrepl :refer [code]]))

(fact
 "execute-request fails without a running execute process"
 (log/with-level :error
   (do (init/ensure-init-global-state!)
       (with-open [srv (srv/make-cljsrv)]
         (let [code "(list 1 2 3)"
               jup (apply jup/make-jup (repeatedly 8 #(async/chan 25)))
               msg ((ts/s*message-header msgs/EXECUTE-REQUEST)
                    (merge (ts/default-execute-request-content) {:code code}))
               port :shell_port
               req {:req-message msg, :req-port port, :cljsrv srv, :jup jup}
               ;; note: execute/, not dispatch/: -- execute is in a separate process
               {:keys [enter-action leave-action] :as rsp} (execute/eval-request req)
               enter-specs (a/step-specs enter-action)
               [input-spec & input-rest] (->> enter-specs (filter (C :msgtype (p = msgs/EXECUTE-INPUT))))
               leave-specs (a/step-specs leave-action)
               [reply-spec & reply-rest] (->> leave-specs (filter (C :msgtype (p = msgs/EXECUTE-REPLY))))
               [result-spec & result-rest] (->> leave-specs (filter (C :msgtype (p = msgs/EXECUTE-RESULT))))
               [history-spec & history-rest] (->> leave-specs (filter (C :op (p = :add-history))))
               [exe-count-spec & exe-count-rest] (->> leave-specs (filter (C :op (p = :inc-execute-count))))
               {input-message-to :message-to, input-msgtype :msgtype,
                input-message :message} input-spec
               {input-exe-count :execution_count} input-message
               {reply-message-to :message-to, reply-msgtype :msgtype,
                reply-message :message} reply-spec
               {reply-exe-count :execution_count, reply-status :status} reply-message
               {result-message-to :message-to, result-msgtype :msgtype,
                result-message :message} result-spec
               {result-exe-count :execution_count} result-message
               {history-data :data} history-spec]
           (and (sh/successful-action? enter-action)
                (sh/successful-action? enter-action)
                ;; INPUT-MSG
                :input-msg
                (nil? input-rest)
                (= input-message-to :iopub_port)
                (= input-msgtype msgs/EXECUTE-INPUT)
                (s/valid? ::msp/execute-input-content input-message)
                :exe-count
                (nil? exe-count-rest)
                (integer? input-exe-count)
                (= input-exe-count reply-exe-count result-exe-count)
                ;; REPLY-MSG
                :reply-msg
                (nil? reply-rest)
                (= reply-message-to port)
                (= reply-msgtype msgs/EXECUTE-REPLY)
                (s/valid? ::msp/execute-reply-content reply-message)
                ;; RESULT-MSG
                :result-msg
                (nil? result-rest)
                (= result-message-to :iopub_port)
                (= result-msgtype msgs/EXECUTE-RESULT)
                (s/valid? ::msp/execute-result-content result-message)
                ;; HISTORY + EXE-COUNTER
                :history
                (nil? history-rest)
                (= code history-data)
                :status
                (= "ok" reply-status))))))
 => true)

(fact
 "read-line works and is correctly ordered wrt other side-effects"
 (log/with-level :error
    (let [[ctrl-in ctrl-out shell-in shell-out io-in io-out stdin-in stdin-out]
          ,, (repeatedly 8 #(async/chan 25))
          jup (jup/make-jup ctrl-in ctrl-out shell-in shell-out io-in io-out stdin-in stdin-out)]
      (async/>!! stdin-in {:req-message ((ts/s*message-header msgs/INPUT-REPLY) (msgs/input-reply-content "input-1"))})
      (async/>!! stdin-in {:req-message ((ts/s*message-header msgs/INPUT-REPLY) (msgs/input-reply-content "input-2"))})
      (init/ensure-init-global-state!)
      (with-open [srv (srv/make-cljsrv)]
        (let [code (code (println (list 1 2 3))
                         (println (read-line))
                         (println 123)
                         (println (str "Read from stdin: " (read-line)))
                         14717)
              msg ((ts/s*message-header msgs/EXECUTE-REQUEST)
                   (merge (ts/default-execute-request-content) {:code code}))
              port :shell_port
              req {:req-message msg, :req-port port, :cljsrv srv, :jup jup}
              {:keys [leave-action]} (execute/eval-request req)
              specs (a/step-specs leave-action)]
          (.invoke leave-action)
          (= ["(1 2 3)\n" "input-1\n" "123\n" "Read from stdin: input-2\n"]
             (->> (core-test/on-outbound-channels jup)
                  (map (P dissoc :req-message))
                  (filter (C :rsp-msgtype (P = "stream")))
                  (filter (C :rsp-content :name (P = "stdout")))
                  (mapv (C :rsp-content :text))))))))
 => true)

;; Nobody is calling the display fn, except this test right here.
#_
(fact
 "Calling `display` yields `display_message` on `:iopub_port"
 (log/with-level :error
   (let [[ctrl-in ctrl-out shell-in shell-out io-in io-out stdin-in stdin-out]
         ,, (repeatedly 8 #(async/chan 25))
         jup (jup/make-jup ctrl-in ctrl-out shell-in shell-out io-in io-out stdin-in stdin-out)]
     (init/ensure-init-global-state!)
     (with-open [srv (srv/make-cljsrv)]
       (let [code (code (clojupyter.display/display 123))
             msg ((ts/s*message-header msgs/EXECUTE-REQUEST)
                  (merge (ts/default-execute-request-content) {:code code}))
             port :shell_port
             ctx {:req-message msg, :req-port port, :cljsrv srv, :jup jup}
             {:keys [leave-action]} (clojupyter.state/with-current-context [ctx]
                                      (execute/eval-request ctx))
             specs (a/step-specs leave-action)
             interpr-spec (first specs)
             display-specs (->> specs (filter (C :msgtype (p = msgs/DISPLAY-DATA))) )
             display-spec (first display-specs)]
         (and (= "ok" (-> interpr-spec :interpretation :reply :status))
              (= 1 (count display-specs))
              (= {"text/plain" "123"} (-> display-spec :message :data))
              (= :iopub_port (:message-to display-spec)))))))
 => true)
