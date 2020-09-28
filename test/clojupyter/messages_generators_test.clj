(ns clojupyter.messages-generators-test
  (:require [clojupyter]
            [clojupyter.messages :as msgs]
            [clojupyter.messages-specs :as msp]
            [clojupyter.test-shared :as ts]
            [clojupyter.test-shared-generators :as shg :refer [R]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [io.simplect.compose :refer [C def- P p]]))

(def- g-somedata (R {:some-key 42, :state {:another-key 99} :buffer_paths []}))
(def- g-std-name (shg/g-name 2 10))
(def- g-std-exe-count (gen/choose 1 1000))
(def- g-reply-status (gen/frequency [[8 (gen/elements ["ok"])]
                                     [1 (gen/elements ["error"])]]))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; MESSAGE TYPE GENERATOR
;;; ------------------------------------------------------------------------------------------------------------------------

(def g-msgtype
  (gen/elements [msgs/COMM-CLOSE
                 msgs/COMM-INFO-REPLY
                 msgs/COMM-INFO-REQUEST
                 msgs/COMM-MSG
                 msgs/COMM-OPEN
                 msgs/COMPLETE-REPLY
                 msgs/COMPLETE-REQUEST
                 msgs/ERROR
                 msgs/EXECUTE-INPUT
                 msgs/EXECUTE-REPLY
                 msgs/EXECUTE-REQUEST
                 msgs/EXECUTE-RESULT
                 msgs/HISTORY-REPLY
                 msgs/HISTORY-REQUEST
                 msgs/INPUT-REQUEST
                 msgs/INPUT-REPLY
                 msgs/INSPECT-REPLY
                 msgs/INSPECT-REQUEST
                 msgs/INTERRUPT-REPLY
                 msgs/INTERRUPT-REQUEST
                 msgs/IS-COMPLETE-REPLY
                 msgs/IS-COMPLETE-REQUEST
                 msgs/KERNEL-INFO-REPLY
                 msgs/KERNEL-INFO-REQUEST
                 msgs/SHUTDOWN-REPLY
                 msgs/SHUTDOWN-REQUEST
                 msgs/STATUS
                 msgs/STREAM]))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; MESSAGE HEADER GENERATOR
;;; ------------------------------------------------------------------------------------------------------------------------

(defn g-message-header
  [msgtype]
  (gen/let [message-id shg/g-uuid
            username g-std-name
            session shg/g-uuid
            date (gen/fmap #(str "DATE: " %) g-std-name)
            version shg/g-version]
    (R (msgs/make-jupmsg-header message-id msgtype username session date version))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; MESSAGE GENERATORS
;;; ------------------------------------------------------------------------------------------------------------------------

(def g-comm-close-content
  (gen/let [id shg/g-uuid
            data g-somedata]
    (R {:msgtype msgs/COMM-CLOSE
        :content (->> (msgs/comm-close-content id data)
                      (s/assert ::msp/comm-close-content))})))

(def g-comm-info-reply-content
  (gen/let [n (gen/choose 0 10)
            ids (gen/vector shg/g-uuid n)
            nms (gen/vector g-std-name n)]
    (R {:msgtype msgs/COMM-INFO-REPLY
        :content (->> (msgs/comm-info-reply-content (zipmap ids nms))
                      (s/assert ::msp/comm-info-reply-content))})))

(def g-comm-info-request-content
  (gen/let [nm g-std-name]
    (R {:msgtype msgs/COMM-INFO-REQUEST
        :content (->> (msgs/comm-info-request-content nm)
                      (s/assert ::msp/comm-info-request-content))})))

(def g-comm-message-content
  (gen/let [id shg/g-uuid
            data g-somedata]
    (R {:msgtype msgs/COMM-MSG
        :content (->> (msgs/comm-msg-content id data)
                      (s/assert ::msp/comm-message-content))})))

(def g-comm-open-content
  (gen/let [id shg/g-uuid
            modnm (shg/g-name 2 10)
            tgtnm (shg/g-name 2 10)
            data g-somedata]
    (R {:msgtype msgs/COMM-OPEN
        :content (->> (msgs/comm-open-content id data {:target_module modnm, :target_name tgtnm})
                      (s/assert ::msp/comm-open-content))})))

(def g-complete-reply-content
  (gen/let [matches (gen/such-that (C count (p >)) (gen/vector g-std-name))
            minlen (R (if (-> matches count zero?) 0 (apply min (map count matches))))
            maxlen (R (if (-> matches count zero?) 0 (apply max (map count matches))))
            cursor-end (gen/choose 0 maxlen)
            cursor-start (gen/choose 0 cursor-end)]
    (R {:msgtype msgs/COMPLETE-REPLY
        :content (->> (msgs/complete-reply-content matches cursor-start cursor-end)
                      (s/assert ::msp/complete-reply-content))})))

(def g-complete-request-content
  (gen/let [codestr (gen/frequency [[5 (gen/elements ["(println )"])]
                                    [1 shg/g-safe-code-string]])
            pos (gen/choose 0 10)]
    (R {:msgtype msgs/COMPLETE-REQUEST
        :content (->> (msgs/complete-request-content codestr pos)
                      (s/assert ::msp/complete-request-content))})))

(def g-error-message-content
  (gen/let [n g-std-exe-count]
    (R {:msgtype msgs/ERROR
        :content (->> (msgs/error-message-content n)
                      (s/assert ::msp/error-message-content))})))

(def g-execute-input-message-content
  (gen/let [n g-std-exe-count
            codestr shg/g-safe-code-string]
    (R {:msgtype msgs/EXECUTE-INPUT
        :content (->> (msgs/execute-input-msg-content n codestr)
                      (s/assert ::msp/execute-input-content))})))

(def g-execute-reply-content
  (gen/let [status g-reply-status
            n g-std-exe-count
            ename (gen/elements [nil {:ename "ENAME-HERE"}])
            evalue (if ename
                     (gen/elements [{:evalue "EVALUE-HERE"}])
                     (gen/elements [{}]))
            traceback (if ename
                        (gen/elements [{:traceback "TRACEBACK-HERE"}])
                        (gen/elements [{}]))]
    (R {:msgtype msgs/EXECUTE-REPLY
        :content (->> (msgs/execute-reply-content status n
                                                  (merge {} ename evalue traceback))
                      (s/assert ::msp/execute-reply-content))})))

(def g-execute-request-content
  (gen/let [allow-stdin? gen/boolean
            silent? gen/boolean
            stop-on-error? gen/boolean
            store-history? gen/boolean
            code shg/g-safe-code-string]
    (R {:msgtype msgs/EXECUTE-REQUEST
        :content (->> (msgs/execute-request-content code allow-stdin? silent? stop-on-error? store-history?)
                      (s/assert ::msp/execute-request-content))})))

(def g-execute-result-content
  (gen/let [data g-somedata
            n g-std-exe-count]
    (R {:msgtype msgs/EXECUTE-RESULT
        :content (->> (msgs/execute-result-content data n)
                      (s/assert ::msp/execute-result-content))})))

(def g-history-reply-content
  (let [histmaps '({:session 1, :line 1, :source "(list 1 2 3)"}
                   {:session 1, :line 2, :source "(list 4 5 6)"}
                   {:session 1, :line 3, :source "(println :ok)"}
                   {:session 1, :line 4, :source "(* 999 888 77)"})]
    (R {:msgtype msgs/HISTORY-REPLY
        :content (->> (msgs/history-reply-content histmaps)
                      (s/assert ::msp/history-reply-content))})))

(def g-history-request-content
  (R {:msgtype msgs/HISTORY-REQUEST
      :content (msgs/history-request-content)}))

(def g-input-reply-content
  (gen/let [v g-std-name]
    (R {:msgtype msgs/INPUT-REPLY
        :content (msgs/input-reply-content v)})))

(def g-input-request-content
  (gen/let [prompt (gen/fmap (P str ":") g-std-name)
            password gen/boolean]
    (R {:msgtype msgs/INPUT-REQUEST
        :content (->> (msgs/input-request-content prompt password)
                      (s/assert ::msp/input-request-content))})))

(def g-inspect-reply-content
  (gen/let [code-str shg/g-safe-code-string
            result-str (gen/elements ["RESULT-HERE"])]
    (R {:msgtype msgs/INSPECT-REPLY
        :content (->> (msgs/inspect-reply-content code-str result-str)
                      (s/assert ::msp/inspect-reply-content))})))

(def g-inspect-request-content
  (gen/let [code shg/g-safe-code-string
            pos (gen/choose 0 (count code))]
    (R {:msgtype msgs/INSPECT-REQUEST
        :content (->> (msgs/inspect-request-content code pos)
                      (s/assert ::msp/inspect-request-content))})))

;;(def g-interrupt-reply-content)	;; NOT IMPLEMENTED
;;(def g-interrupt-request-content)	;; NOT IMPLEMENTED

(def g-is-complete-reply-content
  (gen/let [status g-reply-status]
    (R {:msgtype msgs/IS-COMPLETE-REPLY
        :content (->> (msgs/is-complete-reply-content status)
                      (s/assert ::msp/is-complete-reply-content))})))

(def g-is-complete-request-content
  (gen/let [codestr shg/g-safe-code-string
            len (gen/one-of [(gen/elements [(count codestr)])
                             (gen/choose 0 (dec (count codestr)))])
            codesubstr (R (subs codestr 0 len))]
    (R {:msgtype msgs/IS-COMPLETE-REQUEST
        :content (->> (msgs/is-complete-request-content codesubstr)
                      (s/assert ::msp/is-complete-request-content))})))

(def g-kernel-info-reply-content
  (gen/let [banner (shg/g-nilable g-std-name)
            clj-ver (shg/g-nilable (gen/elements ["1.2.3"]))
            impl (shg/g-nilable (gen/elements ["some-other-impl"]))
            proto-ver (shg/g-nilable (gen/frequency [[10 (gen/elements [msgs/PROTOCOL-VERSION])]
                                                    [1 (gen/elements ["5.1" "5.2" "5.3 " "4.0" "5.8" "7.0" "99.1"])]]))
            version-str (shg/g-nilable (gen/frequency [[10 (gen/elements [clojupyter/version])]
                                                      [1 (gen/elements ["0.0.0" "1.2.3" "2.3.4" "5.6.0"])]]))]
    (R {:msgtype msgs/KERNEL-INFO-REPLY
        :content (->> (msgs/kernel-info-reply-content msgs/PROTOCOL-VERSION
                                                      {:banner banner
                                                       :clj-ver clj-ver
                                                       :implementation impl
                                                       :protocol-version proto-ver
                                                       :version-string version-str})
                      (s/assert ::msp/kernel-info-reply-content))})))

(def g-kernel-info-request-content
  (R {:msgtype msgs/KERNEL-INFO-REQUEST
      :content (->> (msgs/kernel-info-request-content)
                    (s/assert ::msp/kernel-info-request-content))}))

(def g-shutdown-reply
  (gen/let [restart? gen/boolean]
    (R {:msgtype msgs/SHUTDOWN-REPLY
        :content (->> (msgs/shutdown-reply-content restart?)
                      (s/assert ::msp/shutdown-reply-content))})))

(def g-shutdown-request
  (gen/let [restart? gen/boolean]
    (R {:msgtype msgs/SHUTDOWN-REQUEST
        :content (->> (msgs/shutdown-request-content restart?)
                      (s/assert ::msp/shutdown-request-content))})))

(def g-status-message-content
  (gen/let [state (gen/elements ["busy" "idle" "starting"])]
    (R {:msgtype msgs/STATUS
        :content (->> (msgs/status-message-content state)
                      (s/assert ::msp/status-message-content))})))

(def g-stream-message-content
  (gen/let [nm g-std-name
            text g-std-name]
    (R {:msgtype msgs/STREAM
        :content (->> (msgs/stream-message-content nm text)
                      (s/assert ::msp/stream-message-content))})))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; MULTI-MESSAGE GENERATORS
;;; ------------------------------------------------------------------------------------------------------------------------

(def g-jupmsg-content-any
  (gen/one-of [
               g-comm-close-content
               g-comm-info-reply-content
               g-comm-info-request-content
               g-comm-message-content
               g-comm-open-content
               g-complete-reply-content
               g-complete-request-content
               g-error-message-content
               g-execute-input-message-content
               g-execute-reply-content
               g-execute-request-content
               g-execute-result-content
               g-history-reply-content
               g-history-request-content
               g-input-request-content
               g-inspect-reply-content
               g-inspect-request-content
               g-is-complete-reply-content
               g-is-complete-request-content
               g-kernel-info-reply-content
               g-kernel-info-request-content
               g-shutdown-reply
               g-shutdown-request
               g-status-message-content
               g-stream-message-content
               ]))

(def g-jupmsg-any
  (gen/let [{:keys [content msgtype]} g-jupmsg-content-any
            envelope (shg/g-byte-arrays 0 0 1 5)
            signature (shg/g-byte-array 10 20)
            hdr (g-message-header msgtype)
            phdr (g-message-header msgtype)
            metadata (R {})
            buffers (R [])]
    (R (msgs/make-jupmsg envelope signature hdr phdr metadata content buffers ))))
