(ns clojupyter.util
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [io.simplect.compose :refer [def- redefn]]
            [pandect.algo.sha256 :refer [sha256-hmac]]
            [clojupyter.log :as log]))

(def- CHARSET "UTF8")

(def json-str cheshire/generate-string)

(defn bytes->string
  [bytes]
  (String. bytes "UTF-8"))

(defn string->bytes
  [v]
  (cond
    (= (type v) (Class/forName "[B"))
    ,, v
    (string? v)
    ,, (.getBytes ^String v CHARSET)
    :else
    ,, (.getBytes (json-str v) CHARSET)))

;;; ----------------------------------------------------------------------------------------------------
;;; EXTERNAL
;;; ----------------------------------------------------------------------------------------------------

(def IDSMSG "<IDS|MSG>")
(def IDSMSG-BYTES (string->bytes IDSMSG))
(def SEGMENT-ORDER [:envelope :delimiter :signature :header :parent-header :metadata :content])

(defn code-empty?
  [s]
  (-> s str str/trim count zero?))

(defn code-hushed?
  [s]
  (-> s str str/trimr (str/ends-with? ";")))

(def delimiter-frame?
  "Accepts a single argument which must be a byte-array, returns `true` the array represents the
  Jupyter 'delimiter' frame (consisting of the bytes of the text '<IDS|MSG>' encoded as UTF-8);
  otherwise returns `false`."
  (let [delim ^bytes IDSMSG-BYTES
        n-delim (count delim)]
    (fn [^bytes other-byte-array]
      (if (= n-delim (count other-byte-array))
        (loop [idx 0]
          (cond
            (= idx n-delim)
            ,, true
            (= (aget delim idx) (aget other-byte-array idx))
            ,, (recur (inc idx))
            :else
            ,, false))
        false))))

(defmacro ^{:style/indent :defn} define-simple-record-print
  [name format-fn]
  (if (symbol? name)
    (let [fmtfn (gensym "fn-")]
      `(let [~fmtfn ~format-fn]
         (defmethod print-method ~name
           [rec# w#]
           (.write w# (~fmtfn rec#)))
         (defmethod pp/simple-dispatch ~name
           [rec#]
           (print (~fmtfn rec#)))))
    (throw (Exception. (str "define-simple-record-print: invalid form")))))


(defn make-signer-checker
  [key]
  (let [mkchecker (fn mkchecker [signer]
                    (fn [{:keys [header parent-header metadata content preframes]}]
                      (let [payload-vec [header parent-header metadata content]
                            signature (.-signature preframes)
                            check-signature (signer payload-vec)]
                        (log/debug "Checking message with key" key)
                        (or (= "" check-signature)      ; When signer returns "" authentication and message signing is disabled.
                            (= check-signature (bytes->string signature))))))
        signer	(if (empty? key)
                  (constantly "")
                  (fn signer [payload-vec]
                    ;; BUG: The signer fn should not generate its own strings from maps, but it should
                    ;;      check the strings it received from the channel.
                    (sha256-hmac (apply str payload-vec) key)))]
    [signer (mkchecker signer)]))

(redefn parse-json-str cheshire/parse-string)

(defn to-json-str
  "Returns JSON representation (string) of `v`."
  [v]
  (let [repr (java.io.StringWriter.)]
    (cheshire/generate-stream v repr)
    (str repr)))
