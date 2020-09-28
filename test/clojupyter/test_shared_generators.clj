(ns clojupyter.test-shared-generators
  (:require [clojupyter.messages-specs :as msp]
            [clojupyter.test-shared :as ts]
            [clojupyter.util-actions :as u!]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.generators :refer [sample]]
            [io.simplect.compose :refer [C def- p redefn]]))

(def R gen/return)

;;; ----------------------------------------------------------------------------------------------------
;;; INTERNAL
;;; ----------------------------------------------------------------------------------------------------

(def- g-digits*
  (gen/elements '[0 1 2 3 4 5 6 7 8 9]))

(defn- g-name*
  "Generator producing alphanum strings ('names') between 2 and 5 chars long."
  [minlen maxlen]
  (assert (and (<= 0 minlen) (<= minlen maxlen)) "g-name: minlen must be a non-negative integer less than maxlen")
  (gen/fmap (C flatten (p apply str))
            (gen/tuple gen/char-alpha
                       (gen/vector (gen/frequency [[1 (gen/elements [\- \_])]
                                                   [5 gen/char-alphanumeric]])
                                   minlen maxlen))))

(def- g-random-port-number*
  (gen/choose 50000 65535))

(def- g-path*
  "Generator producing file names consisting of 2 to 4 segments generated using `g-name`."
  (gen/let [s (g-name* 2 4)]
    (gen/return (io/file (str "/" (str/join "/" s))))))

(def- g-safe-code-arith-op*
  (gen/elements '[* + -]))

(def- g-safe-code-arith-expr*
  (gen/let [op g-safe-code-arith-op*
            values (gen/vector (gen/frequency [[10 g-digits*]
                                               [1 g-safe-code-arith-expr*]]) 2 7)]
    (R (list* op values))))

(defn- g-string*
  ([minlen maxlen] (g-string* gen/char minlen maxlen))
  ([char-generator minlen maxlen]
   (gen/fmap (p apply str) (gen/vector char-generator minlen maxlen))))

;;; ----------------------------------------------------------------------------------------------------
;;; EXTERNAL
;;; ----------------------------------------------------------------------------------------------------

(defmacro ==>
  "Implies" ;; Can't be a function since we want to not evaluate y when not x
  [x y]
  `(or (not ~x) (boolean ~y)))

(defn <==>
  "If-and-only-if"
  [x y]
  (and (==> x y) (==> y x)))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; SHARED GENERATORS
;;; ------------------------------------------------------------------------------------------------------------------------

(defn g-alphanum
  [minlen maxlen]
  (gen/fmap (p apply str) (gen/vector gen/char-alphanumeric minlen maxlen)))

(defn g-byte-array
  [minsize maxsize]
  (assert (pos? minsize))
  (assert (>= maxsize minsize))
  (gen/let [size (gen/choose minsize maxsize)
            elems (gen/vector gen/byte size)]
    (R (byte-array elems))))

(defn g-byte-arrays
  [min-count max-count minsize maxsize]
  (assert (not (neg? min-count)))
  (assert (>= max-count min-count))
  (gen/let [n (gen/choose min-count max-count)]
    (gen/vector (g-byte-array minsize maxsize) n)))

(defn g-combine-flag-and-val
  [g-flag g-val]
  (gen/let [flag g-flag
            value g-val
            assign? gen/boolean]
    (R [value (cond
                (not flag)
                ,, []
                (re-find #"^--" (str flag))
                ,, (if assign?
                     [(str flag "=" value)]
                     [flag value])
                :else
                ,, [(str flag value)])])))

(defn g-constant
  [v]
  (gen/elements [v]))

(def g-digits g-digits*)

(def g-flag-double
  (gen/fmap (p str "--") (g-alphanum 2 10)))

(def g-flag-single
  (gen/fmap (p str "-") (g-alphanum 1 1)))

(defn g-hex-string
  [minlen maxlen]
  (gen/fmap (p apply str)
            (gen/vector (gen/elements [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9, \a \b \c \d \e \f])
                        minlen maxlen)))

(def g-ident
  (g-name* 1 10))

(def g-jupyter-config
  "Generater jupyter config with random port numbers."
  (gen/let [[ctrl shell stdin iopub hb]
            ,, (gen/such-that (fn [ports] (= (->> ports count) (->> ports (into #{}) count)))
                              (gen/vector g-random-port-number* 5))
            key (g-name* 32 32)]
    (R (->> {:control_port ctrl
             :shell_port shell
             :stdin_port stdin
             :iopub_port iopub
             :hb_port hb
             :ip "127.0.0.1"
             :transport "tcp"
             :key key
             :signature_scheme "hmac-sha256"}
            (s/assert ::msp/jupyter-config)))))

(redefn g-name g-name*)

(def g-nil
  (g-constant nil))

(defn g-nilable
  [g]
  (gen/one-of [(gen/elements [nil]) g]))

(redefn g-path
  g-path*)

(def g-resource
  (gen/fmap (C (p str "file://") #(java.net.URL. %)) g-path))

(def g-safe-code
  (gen/one-of [(gen/elements '[(list 1 2 3) 42])
               g-safe-code-arith-expr*]))

(def g-safe-code-string
  (gen/fmap str g-safe-code))

(redefn g-string
  g-string*)

(def g-uuid
  (gen/let [uuid gen/uuid]
    (R (str uuid))))

(def g-version
  (gen/let [major g-digits*
            minor g-digits*
            incr  g-digits*
            ]
    (R (str major "." minor "." incr))))
