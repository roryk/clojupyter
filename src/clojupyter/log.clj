(ns clojupyter.log
  (:require [clojupyter.kernel.config :as cfg]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [io.simplect.compose :refer [def- C]]
            [taoensso.timbre :as timbre]))

(def ^:dynamic *verbose* false)

(defmacro debug
  [& args]
  `(timbre/debug ~@args))

(defmacro info
  [& args]
  `(timbre/info ~@args))

(defmacro error
  [& args]
  `(timbre/error ~@args))

(defmacro warn
  [& args]
  `(timbre/warn ~@args))

(defmacro with-level
  [level & body]
  `(timbre/with-level ~level ~@body))

(defn ppstr
  [v]
  (with-out-str
    (println)
    (println ">>>>>>>")
    (pp/pprint v)
    (println "<<<<<<<<")))

(defonce ^:private ORG-CONFIG timbre/*config*)

(def- fmt-level
  (C name str/upper-case first))

(defn- fmt-origin
  [?ns-str ?file]
  (str/replace (or ?ns-str ?file "?") #"^clojupyter\." "c8r."))

(defn output-fn
    ([     data] (output-fn nil data))
    ([opts data]
     (let [{:keys [no-stacktrace?]} opts
           {:keys [level ?err msg_ ?ns-str ?file hostname_
                   timestamp_ ?line]} data]
       (str					"["
        (fmt-level level)			" "
        (force timestamp_)			" "
        "Clojupyter"				"] "
        (str (fmt-origin ?ns-str ?file)
             (when ?line (str ":" ?line)))	" -- "
        (force msg_)
        (when-not no-stacktrace?
          (when-let [err ?err]
            (str "\n" (timbre/stacktrace err opts))))))))

(def CONFIG {:timestamp-opts {:pattern "HH:mm:ss.SSS", :locale :jvm-default, :timezone :utc}
             :ns-blacklist ["io.pedestal.*"]
             :output-fn output-fn
             :level :warn})

(defn- set-clojupyter-config!
  []
  (timbre/merge-config! CONFIG))

(defn- reset-config!
  []
  (timbre/set-config! ORG-CONFIG))

(defn init!
  []
  (set-clojupyter-config!)
  (timbre/set-level! (cfg/log-level)))

(init!) ;; avoid spurious debug messages from io.pedestal when loading with midje testing turned on
