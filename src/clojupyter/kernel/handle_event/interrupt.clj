(ns clojupyter.kernel.handle-event.interrupt
  (:require [clojupyter.kernel.cljsrv :as cljsrv]
            [clojupyter.kernel.handle-event.ops :as ops :refer [definterceptor]]
            [clojupyter.messages :as msgs]
            [clojupyter.plan :as plan :refer [s*bind-state]]
            [io.simplect.compose.action :as action :refer [action step]]))

(definterceptor ic*interrupt msgs/INTERRUPT-REQUEST
  (s*bind-state {:keys [cljsrv]}
    (ops/s*append-enter-action (action (step [`cljsrv/nrepl-interrupt cljsrv]
                                             {:op :nrepl-interrupt}))))
  (ops/s*set-response msgs/INTERRUPT-REPLY (msgs/interrupt-reply-content)))
