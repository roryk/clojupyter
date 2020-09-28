(ns clojupyter.kernel.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [omniconf.core :as cfg]
            [io.simplect.compose :refer [def-]]))

(cfg/define
  {:log-level			{:description	"Default log level as defined by com.taoensso/timbre."
                                 :type		:keyword
                                 :one-of	[:trace :debug :info :warn :error :fatal :report]
                                 :default	:error}
   :print-stacktraces?		{:description	(str "Print stacktrace on error. "
                                                     "Temporary workaround for issue with uncaught exceptions in nrepl.")
                                 :type		:boolean
                                 :default	true}
   :log-traffic?		{:description	"Use log/debug to print messages going to/from Jupyter."
                                 :type		:boolean
                                 :default	false}})

;;; ----------------------------------------------------------------------------------------------------
;;; OS SUPPORT
;;; ----------------------------------------------------------------------------------------------------

(defn- osname [] (-> (System/getProperty "os.name") str/lower-case str/trim))
(defn- os? [idstr] (fn [] (not (neg? (.indexOf ^String (osname) ^String idstr)))))

(def- mac?	(os? "mac"))
(def- linux?	(os? "linux"))
(def- windows?	(os? "windows"))

(defn- user-homedir
  []
  (io/file
   (or (System/getProperty "user.home")
       (System/getenv "HOME")
       (throw (Exception. "User home directory not found.")))))

;;; ----------------------------------------------------------------------------------------------------
;;; DATA DIRECTORY
;;; ----------------------------------------------------------------------------------------------------

(def- CLOJUPYTER-DATADIR	"clojupyter")
(def- XDG_CONFIG_HOME		"XDG_CONFIG_HOME")
(def- XDG_DATA_HOME		"XDG_DATA_HOME")
(def- LOCALAPPDATA		"LOCALAPPDATA")

(defn- default-datahome-relative
  []
  (cond
    (mac?)	"Library/Caches"
    (linux?)	".local/share"))

(defn- default-datahome
  []
  (when-let [datahome-rel (default-datahome-relative)]
    (io/file (str (user-homedir) "/" datahome-rel))))

(defn- datahome
  []
  (io/file
   (or (System/getenv XDG_DATA_HOME)
       (System/getenv LOCALAPPDATA)
       (default-datahome))))

(defn clojupyter-datahome
  "Returns the name of the directory which clojupyter will use to store
  its own data, such as the history file."
  []
  (if-let [datahome (datahome)]
    (let [dir (io/file (str datahome "/" CLOJUPYTER-DATADIR))]
      (when-not (.exists dir)
        (.mkdirs dir))
      dir)
    (user-homedir)))


;;; ----------------------------------------------------------------------------------------------------
;;; CONFIGURATION FILE
;;; ----------------------------------------------------------------------------------------------------

(def ^:private CONFIG-FILE		"clojupyter.edn")

(defn- default-config-dir-relative
  []
  (cond
    (mac?)	"Library/Preferences"
    (linux?)	".config"))

(defn- default-config-dir
  []
  (when-let [datahome-rel (default-config-dir-relative)]
    (io/file
     (str (user-homedir) "/" datahome-rel))))

(defn- config-dir
  []
  (io/file
   (or (System/getenv XDG_CONFIG_HOME)
       (System/getenv LOCALAPPDATA)
       (default-config-dir))))

(defn config-file
  "Returns the name of the configuration file which clojupyter will load."
  []
  (when-let [config-dir (config-dir)]
    (io/file (str config-dir "/" CONFIG-FILE))))

(defn init!
  "Loads the configuration file if it exists, then verifies
  configuration.  Returns `:ok` if errors are not found, otherwise
  throws an exception."
  []
  (when-let [config-file ^java.io.File (config-file)]
    (when (.exists config-file)
      (cfg/populate-from-file config-file)))
  (cfg/verify :silent true)
  :ok)

;;; ----------------------------------------------------------------------------------------------------
;;; CONFIG-SPECIFIC
;;; ----------------------------------------------------------------------------------------------------

(defn configuration
  []
  (cfg/get))

(defn print-stacktraces?
  []
  (cfg/get :print-stacktraces?))

(defn log-level
  []
  (cfg/get :log-level))

(defn log-traffic?
  []
  (cfg/get :log-traffic?))
