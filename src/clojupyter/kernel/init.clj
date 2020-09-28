(ns clojupyter.kernel.init
  (:require [clojupyter]
            [clojupyter.kernel.config :as cfg]
            [clojupyter.kernel.stacktrace :as stacktrace]
            [clojupyter.log :as log]
            [clojupyter.state :as state]
            [clojupyter.util-actions :as u!]))

(def INITIALIZED? (atom false))

(defn- shutdown-hook
  []
  (try (u!/with-exception-logging
           (.close (state/zmq-context))
         (log/info "Shutdown-hook terminating."))
       (finally
         (Thread/sleep 10)
         (shutdown-agents))))

(defn- setup-shutdown-hook
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown-hook)))

(defn ensure-init-global-state!
  "Initializes global state."
  []
  (if @INITIALIZED?
    false
    (do (cfg/init!)
        (log/init!)
        (let [ver clojupyter/version]
          (log/info (str "Clojupyter version " ver ".")))
        (when-let [config-file (cfg/config-file)]
          (log/info (str "Configuration read from " config-file "."))
          (log/info (str "Configuration: ") (cfg/configuration)))
        (state/ensure-initial-state!)
        (stacktrace/init!)
        (setup-shutdown-hook)
        (reset! INITIALIZED? true))))
