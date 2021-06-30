(ns clojupyter.util-actions
  (:require [clojupyter.kernel.os :as os]
            [clojupyter.kernel.version :as ver]
            [clojupyter.log :as log]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.stacktrace :as stacktrace]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [io.simplect.compose :refer [C call-if def- p P redefn]]
            [io.simplect.compose.action :as a]
            [java-time :as jtm]
            [me.raynes.fs :as fs]
            [slingshot.slingshot :refer [try+]])
  (:import java.time.format.DateTimeFormatter))

(def ^:dynamic *actually-exit?* true)

(defmacro ^{:style/indent :defn} closing-channels-on-exit!
  [channels & body]
  `(try ~@body
        (finally ~@(for [chan channels]
                     `(async/close! ~chan)))))

(defn exiting-on-completion*
  ;; needs to be external due inclusion in macro expansion
  [thunk]
  (try+ (thunk)
        (catch [::terminate true] {:keys [::exit-code] :as obj}
          (when *actually-exit?*
            (System/exit exit-code)))))

(def- homedir-as-tilde*
  (P str/replace (System/getProperty "user.home") "~"))

(defn- set-indent-style!*
  [var style]
  (alter-meta! var (P assoc :style/indent style)))

(defn- throw-info*
  ([msg] (throw-info* msg {}))
  ([msg m]
   (throw (ex-info msg (assoc m :msg msg)))))

(def- tildeize-filename*
  (call-if (p instance? java.io.File)
           (C fs/normalized str homedir-as-tilde*)))

(defn- uuid*
  "Returns a random UUID as a string."
  []
  (str (java.util.UUID/randomUUID)))

;;; ----------------------------------------------------------------------------------------------------
;;; EXTERNAL
;;; ----------------------------------------------------------------------------------------------------

(defn assert-singleton-collection!
  [v]
  (if (and (coll? v) (-> v count (= 1)))
    v
    (throw (Exception. (str "Not a singleton collection:" v)))))

(defn assoc-meta!
  [k v var]
  (alter-meta! var #(assoc % k v)))


(defn create-temp-diretory!
  "Creates a new readable/writable directory and returns its name as a `java.io.File`.  Throws an
  exception if the temp directory could not be created."
  []
  (letfn [(sys-tmpdir [] (io/file (System/getProperty "java.io.tmpdir")))]
    (if-let [sysdir (sys-tmpdir)]
      (let [tmpdir (io/file (str sysdir "/clojure-tmp-" (uuid*)))]
        (if (.mkdir tmpdir)
          tmpdir
          (throw-info* (str "Failed to create temp dir: " tmpdir)
                       {:sysdir sysdir, :tmpdir tmpdir})))
      (throw (Exception. "Failed to get location for temp files (java.io.tmpdir).")))))

(defn delete-files-recursively!
  [f]
  (letfn [(delete-f [^java.io.File file]
            (when (.isDirectory file)
              (doseq [child-file (.listFiles file)]
                (delete-f child-file)))
            (io/delete-file file))]
    (when f
      (if (string? f)
        (delete-files-recursively! (io/file f))
        (delete-f f)))))

(defn default-ident
  ([] (default-ident (ver/version)))
  ([ver]
   (str "clojupyter-" (ver/version-string-long ver))))

(defn execute-leave-action
  [result]
  (if-let [leave-action (:leave-action result)]
    (let [action-result (leave-action {})]
      (if (a/success? action-result)
        (assoc result :leave-action action-result)
        (throw (ex-info (str "Action failed: " action-result)
                 {:action leave-action, :action-result action-result}))))
    result))

(defmacro  exiting-on-completion
  [& body]
  `(exiting-on-completion* (fn [] ~@body)))

(def files-as-strings
  (p walk/postwalk (C (call-if (p instance? java.net.URL) str)
                      tildeize-filename*)))

(defn file-filetype
  [f]
  (cond
    (fs/file? f)		:filetype/file
    (fs/directory? f)		:filetype/directory
    :else			nil))

(defmulti  find-executable (fn [_] (os/operating-system)))
(letfn [(find-exe [exe]
          (let [{:keys [out err exit]} (sh/sh "/usr/bin/env" "which" exe)]
            (when (zero? exit)
              (-> out str/trim io/file)))) ]
  (defmethod find-executable :macos [exe]
    (find-exe exe))
  (defmethod find-executable :linux [exe]
    (find-exe exe)))
(defmethod find-executable :windows [exe]
  (let [{:keys [out err exit] :as search-result} (sh/sh "where" exe)]
    (when (zero? exit)
      (-> out str/trim str/split-lines first io/file))))
(defmethod find-executable :default [_]
  (throw (Exception. (str "find-executable: Not supported on " (str/upper-case (os/osname))))))

(redefn homedir-as-tilde homedir-as-tilde*)

(defmacro ignore-exceptions
  [& exprs]
  `(try (do ~@exprs)
        (catch Exception e# nil)))

(defn java-util-data-now
  []
  (new java.util.Date))

(defn merge-arglists-meta
  "Returns function which merges `:arglists` meta from `from-var`.

  Example use:

    (alter-meta! #'to-var (u!/merge-arglists-meta #'from-var))"
  [from-var]
  #(merge % (select-keys (meta from-var) [:arglists])))

(defn merge-docstring-meta
  "Returns function which merges `:doc` meta from `from-var`.

  Example use:

    (alter-meta! #'to-var (u!/merge-arglists-docstring #'from-var))"
  [from-var]
  #(merge % (select-keys (meta from-var) [:doc])))

(def normalized-file
  (call-if (complement nil?) (C io/file fs/normalized)))

(defn now []
  (->> (.withNano (java.time.ZonedDateTime/now) 0)
       (jtm/format DateTimeFormatter/ISO_OFFSET_DATE_TIME)))

(defn set-defn-indent!
  [& vars]
  (doseq [var vars]
    (set-indent-style!* var :defn)))

(redefn set-indent-style! set-indent-style!*)

(defn set-var-private!
  [var]
  (alter-meta! var (P assoc :private true)))

(defn set-var-indent!
  [indent-style var]
  (alter-meta! var #(assoc % :style/indent indent-style)))

(defn side-effecting!
  "Returns argument after applying presumably side-effectful `f` to it."
  [f]
  (fn [S] (f) S))

(redefn ^{:style/indent :defn} throw-info throw-info*)

(redefn tildeize-filename tildeize-filename*)

(redefn uuid uuid*)

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
        [return-value (str e) e])))))

(set-defn-indent! #'exiting-on-completion #'without-exiting #'with-temp-directory!)
