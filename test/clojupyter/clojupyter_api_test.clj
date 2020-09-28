(ns clojupyter.clojupyter-api-test
  (:require [clojupyter.display]
            [clojupyter.kernel.cljsrv :as srv]
            [clojupyter.kernel.handle-event.execute :as exe]
            [clojupyter.kernel.handle-event.shared-ops :as sh]
            [clojupyter.kernel.jup-channels :as jup]
            [clojupyter.log :as log]
            [clojupyter.messages :as msgs]
            [clojupyter.test-shared :as ts]
            [clojure.core.async :as async]
            [io.simplect.compose :refer [C p]]
            [io.simplect.compose.action :as a]
            [midje.sweet :as midje :refer [=> fact]]))

(defn- eval-code
  [code]
  (log/with-level :error
    (with-open [srv (srv/make-cljsrv)]
      (let [jup (apply jup/make-jup (repeatedly 8 #(async/chan 25)))
            msg ((ts/s*message-header msgs/EXECUTE-REQUEST)
                 (merge (ts/default-execute-request-content) {:code code}))
            port :shell_port
            req {:req-message msg, :req-port port :cljsrv srv, :jup jup}
            {:keys [enter-action leave-action] :as rsp} (exe/eval-request req)]
        (assert (sh/successful-action? enter-action))
        rsp))))

(fact
   "Evaluating an expression yields result tagged as `text/plain`."
   (let [{:keys [leave-action] :as rsp} (eval-code "(list 1 2 3)")
         specs (a/step-specs leave-action)
         [result-spec & result-rest] (->> specs (filter (C :msgtype (p = msgs/EXECUTE-RESULT))))
         {:keys [message-to msgtype message]} result-spec
         {:keys [:data :metadata]} message
         {:keys [:text/plain :text/html]} data
         ]
     (and (boolean result-spec)
          (nil? result-rest)
          (= {} metadata)
          (= plain "(1 2 3)")
          (nil? html)))
   => true)


(fact
 "Using `hiccup` yields result tagged as `text/html`."
 (let [code "(clojupyter.display/hiccup [:p \"para-content\"])"
       {:keys [leave-action] :as rsp} (eval-code code)
       specs (a/step-specs leave-action)
       [result-spec & result-rest] (->> specs (filter (C :msgtype (p = msgs/EXECUTE-RESULT))))
       {:keys [message-to msgtype message]}  result-spec
       {:keys [:data]} message
       {:keys [:text/plain :text/html]} data]
   (and (nil? result-rest)
        (nil? plain)
        (= message-to :iopub_port)
        (= msgtype msgs/EXECUTE-RESULT)
        (= html "<p>para-content</p>")))
 => true)

(fact
 "Using `render-mime` yields result tagged as the indicated mime-type."
 (let [code "(clojupyter.display/render-mime \"text/some-mimetype\" \"somestring\")"
       {:keys [leave-action]} (eval-code code)
       specs (a/step-specs leave-action)
       [result-spec & _] (->> specs (filter (C :msgtype (p = msgs/EXECUTE-RESULT))))
       {:keys [message]}  result-spec
       {:keys [:data]} message
       {:keys [:text/plain :text/html :text/some-mimetype]} data
       ]
   (and (nil? plain)
        (nil? html)
        (= "somestring" some-mimetype)))
 => true)

(fact
 "A semi-colon at the end of the code means no result is sent to Jupyter."
 (let [{:keys [enter-action leave-action] :as rsp} (eval-code "(list 1 2 3);")
       enter-specs (a/step-specs enter-action)
       leave-specs (a/step-specs leave-action)
       input-specs (->> enter-specs (filter (C :msgtype (p = msgs/EXECUTE-INPUT))))
       result-specs (->> leave-specs (filter (C :msgtype (p = msgs/EXECUTE-RESULT))))
       reply-specs (->> leave-specs (filter (C :msgtype (p = msgs/EXECUTE-REPLY))))]
   (and (-> result-specs count zero?)
         (-> reply-specs count pos?)
         (-> input-specs count pos?)))
 => true)

(fact
 "The silencing semi-colon can be followed by whitespace."
 (let [{:keys [enter-action leave-action] :as rsp} (eval-code "(list 1 2 3); \t\n\r")
       enter-specs (a/step-specs enter-action)
       leave-specs (a/step-specs leave-action)
       input-specs (->> enter-specs (filter (C :msgtype (p = msgs/EXECUTE-INPUT))))
       result-specs (->> leave-specs (filter (C :msgtype (p = msgs/EXECUTE-RESULT))))
       reply-specs (->> leave-specs (filter (C :msgtype (p = msgs/EXECUTE-REPLY))))]
   (and (-> result-specs count zero?)
        (-> reply-specs count pos?)
        (-> input-specs count pos?)))
 => true)
