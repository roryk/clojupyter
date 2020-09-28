(ns clojupyter.util-actions
  (:require [clojupyter.log :as log]
            [clojure.core.async :as async]
            [io.simplect.compose :refer [P]]
            [io.simplect.compose.action :as a]
            [java-time :as jtm])
  (:import java.time.format.DateTimeFormatter))

(defmacro ^{:style/indent :defn} closing-channels-on-exit!
  [channels & body]
  `(try ~@body
        (finally ~@(for [chan channels]
                     `(async/close! ~chan)))))

(defn uuid
  "Returns a random UUID as a string."
  []
  (str (java.util.UUID/randomUUID)))

(defn- set-indent-style!
  [var style]
  (alter-meta! var (P assoc :style/indent style)))

;;; ----------------------------------------------------------------------------------------------------
;;; EXTERNAL
;;; ----------------------------------------------------------------------------------------------------

(defn execute-leave-action
  [result]
  (if-let [leave-action (:leave-action result)]
    (let [action-result (leave-action {})]
      (if (a/success? action-result)
        (assoc result :leave-action action-result)
        (do (log/error "Action failed: " (pr-str (.failure action-result)))
            (when log/*verbose* (log/warn {:action leave-action, :action-result action-result}))
            result)))
    result))

(defmacro  exiting-on-completion
  [& body]
  `(exiting-on-completion* (fn [] ~@body)))

(defn java-util-data-now
  []
  (new java.util.Date))

(defn now []
  (->> (.withNano (java.time.ZonedDateTime/now) 0)
       (jtm/format DateTimeFormatter/ISO_OFFSET_DATE_TIME)))

(defn set-defn-indent!
  [& vars]
  (doseq [var vars]
    (set-indent-style! var :defn)))

(defn set-var-private!
  [var]
  (alter-meta! var (P assoc :private true)))

(defn set-var-indent!
  [indent-style var]
  (alter-meta! var #(assoc % :style/indent indent-style)))

(defmacro without-exiting
  [& body]
  `(binding [*actually-exit?* false]
     ~@body))

(defn- with-exception-logging*
  ([form finally-form]
   `(try ~form
         (catch Exception e#
           (do (log/error e#)
               (throw e#)))
         (finally ~finally-form))))

(defmacro ^{:style/indent 1} with-exception-logging
  ([form]
   (with-exception-logging* form '(do)))
  ([form finally-form]
   (with-exception-logging* form finally-form)))

(defmacro with-temp-directory!
  "Evaluates `body` with `dir` bound to a newly created, readable and writable directory.  Upon exit
  all files and directories in the temp directory are deleted unless `keep-files?` has a truthy
  value."
  [[dir & {:keys [keep-files?]}] & body]
  `(let [tmpdir# (create-temp-diretory!), ~dir tmpdir#]
     (try (do ~@body)
          (finally
            (when-not ~keep-files?
              (delete-files-recursively! tmpdir#))))))

(defn assoc-meta!
  [k v var]
  (alter-meta! var #(assoc % k v)))

(defn wrap-report-and-absorb-exceptions
  ([f]
   (wrap-report-and-absorb-exceptions nil f))
  ([return-value f]
   (fn [& args]
     (try (with-exception-logging
              (apply f args))
      (catch Exception e
        (log/error e)
        (Thread/sleep 10)
        (assoc return-value :error e))))))
(set-defn-indent! #'exiting-on-completion #'without-exiting #'with-temp-directory!)
