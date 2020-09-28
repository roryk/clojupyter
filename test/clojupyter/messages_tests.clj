(ns clojupyter.messages-tests
  (:require [clojupyter.messages :as msgs :refer [jupmsg->frames]]
            [clojupyter.messages-generators-test :as mg]
            [clojupyter.test-shared :as ts]
            [clojupyter.test-shared-generators :as shg]
            [clojupyter.messages-specs :as msp]
            [clojupyter.jupmsg-specs :as jsp]
            [clojupyter.test-shared :as sh]
            [clojupyter.util :as u]
            [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen :refer [sample]]
            [clojure.test.check.properties :as prop]
            [io.simplect.compose :refer [def- c C p P]]
            [midje.sweet :refer [fact =>]]))

(def QC-ITERS 500)
(def QC-ITERS-LOW 50)

(def prop--jupyter-protocol-frames-can-be-generated
  (prop/for-all [{:keys [msgtype content]} mg/g-jupmsg-content-any]
    (let [jupmsg (merge ((sh/s*message-header msgtype) content))]
      (and (s/valid? ::jsp/jupmsg jupmsg)
           (s/valid? ::msp/frames (msgs/jupmsg->frames (constantly "-SIGNED-") jupmsg))))))

(fact
 "Jupyter protocol frames can be generated from jupmsgs"
 (:pass? (tc/quick-check QC-ITERS prop--jupyter-protocol-frames-can-be-generated))
 => true)

(def prop--jupmsgs-can-be-round-tripped
  (let [sig (u/string->bytes "-SIGNATURE-")]
    (prop/for-all [{:keys [msgtype content]} mg/g-jupmsg-content-any]
      (let [checker (constantly true)
            jupmsg (merge ((sh/s*message-header msgtype {:signature sig}) content))
            frames(msgs/jupmsg->frames (constantly "-SIGNATURE-") jupmsg)
            jupmsg' (msgs/frames->jupmsg checker frames)]
        (and (apply = (mapv (P dissoc :preframes :buffers) [jupmsg jupmsg']))
             (= [(into [] (-> jupmsg :preframes .-envelope))
                 (into [] (-> jupmsg :preframes .-signature))
                 (into [] (-> jupmsg :preframes .-delimiter))]
                [(into [] (-> jupmsg' :preframes .-envelope))
                 (into [] (-> jupmsg' :preframes .-signature))
                 (into [] (-> jupmsg' :preframes .-delimiter))]))))))

(fact
 "jupmsgs can be round-tripped to frames and back without loss"
 (:pass? (tc/quick-check QC-ITERS prop--jupmsgs-can-be-round-tripped))
 => true)

(def prop--message-accessors-for-execute-requests-appear-to-work
  (prop/for-all [m mg/g-execute-request-content]
    (and (string? (msgs/message-code m))
         (boolean? (msgs/message-allow-stdin m))
         (boolean? (msgs/message-silent m))
         (boolean? (msgs/message-store-history? m))
         (boolean? (msgs/message-stop-on-error? m))
         (map? (msgs/message-user-expressions m)))))

(fact
 "Message accessors for execute-request content appear to work"
 (:pass? (tc/quick-check QC-ITERS-LOW prop--message-accessors-for-execute-requests-appear-to-work))
 => true)

(def prop--message-accessors-for-comm-messages-appear-to-work
  (prop/for-all [m mg/g-comm-message-content]
    (string? (msgs/message-comm-id m))))

(fact
 "Message accessors for comm-messages content appear to work"
 (:pass? (tc/quick-check QC-ITERS-LOW prop--message-accessors-for-comm-messages-appear-to-work))
 => true)

(def prop--message-accessors-for-input-reply-appear-to-work
  (prop/for-all [m mg/g-input-reply-content]
    (string? (msgs/message-value m))))

(fact
 "Message accessors for input-reply content appear to work"
 (:pass? (tc/quick-check QC-ITERS-LOW prop--message-accessors-for-input-reply-appear-to-work))
 => true)

(def prop--message-accessors-for-is-complete-appear-to-work
  (prop/for-all [m mg/g-is-complete-request-content]
    (integer? (msgs/message-cursor-pos m))))

(fact
 "Message accessors for is-complete request content appear to work"
 (:pass? (tc/quick-check QC-ITERS-LOW prop--message-accessors-for-is-complete-appear-to-work))
 => true)

(def prop--message-metadata-accessors-appear-to-work
  (let [mkarrays #(shg/g-byte-arrays 1 3 10 20)
        tgen (gen/let [sig (gen/return (u/string->bytes "-SIGNATURE-"))
                       envelope (mkarrays)
                       buffers (mkarrays)
                       mt mg/g-msgtype
                       hdr (mg/g-message-header mt)
                       pmt mg/g-msgtype
                       phdr (mg/g-message-header pmt)
                       metadata (gen/return {:val (gensym)})
                       content (gen/return {:content (gensym)})]
               (let [jupmsg (msgs/make-jupmsg envelope sig hdr phdr metadata content buffers)]
                 (gen/return
                  (and (= (msgs/message-msg-type jupmsg) mt)
                       (= (msgs/message-session jupmsg) (:session hdr))
                       (= (msgs/message-username jupmsg) (:username hdr))
                       (= (msgs/message-header jupmsg) hdr)
                       (= (msgs/message-parent-header jupmsg) phdr)
                       (= (msgs/message-metadata jupmsg) metadata)
                       (= (msgs/message-content jupmsg) content)
                       (= (mapv (p into []) (msgs/message-buffers jupmsg))
                          (mapv (p into []) buffers))))))]
    (prop/for-all [t tgen] t)))

(fact
 "Message metadata accessors appear to work"
 (:pass? (tc/quick-check QC-ITERS-LOW prop--message-metadata-accessors-appear-to-work))
 => true)

;;; ------------------------------------------------------------------------------------------------------------------------
;;; EXTRACTING AND INSERTING BYTE-ARRAYS (BUFFERS)
;;; ------------------------------------------------------------------------------------------------------------------------

(def prop--values-can-be-extracted-as-paths-and-reinserted-correctly
  (prop/for-all [msg mg/g-jupmsg-any]
    (let [msg (-> msg (dissoc :preframes :buffers))
          [res paths] (msgs/leaf-paths (every-pred (complement map?) (complement vector?)) (constantly :replaced) msg)]
      (and (-> paths count pos?)
           (not= res msg)
           (= msg (msgs/insert-paths res paths))))))

(fact
 "Values can be extracted from messages as paths and reinserted correctly"
 (:pass? (tc/quick-check QC-ITERS prop--values-can-be-extracted-as-paths-and-reinserted-correctly))
 => true)
