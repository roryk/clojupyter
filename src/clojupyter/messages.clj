(ns clojupyter.messages
  (:require [clojupyter.jupmsg-specs :as jsp]
            [clojupyter.specs :as sp]
            [clojupyter]
            [clojupyter.kernel.stacktrace :as stacktrace]
            [clojupyter.log :as log]
            [clojupyter.messages-specs :as msp]
            [clojupyter.util :as u]
            [clojupyter.util-actions :as u!]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [io.simplect.compose :refer [def- C fmap p sdefn]]))

(def PROTOCOL-VERSION "5.2")


;;; ------------------------------------------------------------------------------------------------------------------------
;;; JUPYTER MESSAGE TYPES
;;; ------------------------------------------------------------------------------------------------------------------------

(def CLEAR-OUTPUT		"clear_output")
(def COMM-CLOSE			"comm_close")
(def COMM-INFO-REPLY		"comm_info_reply")
(def COMM-INFO-REQUEST		"comm_info_request")
(def COMM-MSG			"comm_msg")
(def COMM-OPEN			"comm_open")
(def COMPLETE-REPLY		"complete_reply")
(def COMPLETE-REQUEST		"complete_request")
(def DISPLAY-DATA		"display_data")
(def ERROR			"error")
(def EXECUTE-INPUT		"execute_input")
(def EXECUTE-REPLY		"execute_reply")
(def EXECUTE-REQUEST		"execute_request")
(def EXECUTE-RESULT		"execute_result")
(def HISTORY-REPLY		"history_reply")
(def HISTORY-REQUEST		"history_request")
(def INPUT-REQUEST		"input_request")
(def INPUT-REPLY		"input_reply")
(def INSPECT-REPLY		"inspect_reply")
(def INSPECT-REQUEST		"inspect_request")
(def INTERRUPT-REPLY		"interrupt_reply")
(def INTERRUPT-REQUEST		"interrupt_request")
(def IS-COMPLETE-REPLY		"is_complete_reply")
(def IS-COMPLETE-REQUEST	"is_complete_request")
(def KERNEL-INFO-REPLY		"kernel_info_reply")
(def KERNEL-INFO-REQUEST	"kernel_info_request")
(def SHUTDOWN-REPLY		"shutdown_reply")
(def SHUTDOWN-REQUEST		"shutdown_request")
(def STATUS			"status")
(def STREAM			"stream")
(def UPDATE-DISPLAY-DATA	"update_display_data")
,,
(def COMM-MSG-UPDATE		"update")
(def COMM-MSG-REQUEST-STATE	"request_state")
(def COMM-MSG-CUSTOM    "custom")


;;; ----------------------------------------------------------------------------------------------------
;;; MESSAGE ACCESSORS
;;; ----------------------------------------------------------------------------------------------------

(defn message-content		[message]	(get-in message [:content]))
(defn message-allow-stdin	[message]	(get-in message [:content :allow_stdin]))
(defn message-code		[message]	(get-in message [:content :code] ""))
(defn message-comm-id		[message]	(get-in message [:content :comm_id]))
(defn message-comm-data		[message]	(get-in message [:content :data]))
(defn message-comm-method	[message]	(get-in message [:content :data :method]))
(defn message-comm-state	[message]	(get-in message [:content :data :state]))
(defn message-cursor-pos	[message]	(get-in message [:content :cursor_pos] 0))
(defn message-restart		[message]	(boolean (get-in message [:content :restart])))
(defn message-silent		[message]	(get-in message [:content :silent]))
(defn message-stop-on-error?	[message]	(get-in message [:content :stop_on_error]))
(defn message-store-history?	[message]	(if-let [[_ store?] (find (get message :content) :store_history)] store? true))
(defn message-user-expressions	[message]	(get-in message [:content :user_expressions]))
(defn message-value		[message]	(get-in message [:content :value]))
,,
(defn message-header		[message]	(get-in message [:header]))
(defn message-date		[message]	(get-in message [:header :date]))
(defn message-msg-id		[message]	(get-in message [:header :msg_id]))
(defn message-msg-type		[message]	(get-in message [:header :msg_type]))
(defn message-session		[message]	(get-in message [:header :session]))
(defn message-username		[message]	(get-in message [:header :username]))
(defn message-version		[message]	(get-in message [:header :version]))
,,
(defn message-buffers		[message]	(.-buffers (get-in message [:buffers])))
(defn message-delimiter		[message]	(get-in message [:delimiter]))
(defn message-envelope		[message]	(.-envelope (get message :preframes)))
(defn message-metadata		[message]	(get-in message [:metadata]))
(defn message-signature		[message]	(.-signature (get message :preframes)))
,,
(defn message-parent-header	[message]	(get-in message [:parent-header]))
(defn message-parent-date	[message]	(get-in message [:parent-header :date]))
(defn message-parent-msg-id	[message]	(get-in message [:parent-header :msg_id]))
(defn message-parent-msg-type	[message]	(get-in message [:parent-header :msg_type]))
(defn message-parent-session	[message]	(get-in message [:parent-header :session]))
(defn message-parent-username	[message]	(get-in message [:parent-header :username]))
(defn message-parent-version	[message]	(get-in message [:parent-header :version]))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; JupMsgPreframes
;;; ------------------------------------------------------------------------------------------------------------------------

(def- fmtpf-entry
  (C (p into []) hash (p format "0x%x") (p keyword "hash")))

(defn- fmtpf
  [pf]
  (str "#Preframes" [(->> pf .-envelope (mapv fmtpf-entry))
                     (->> pf .-delimiter fmtpf-entry)
                     (->> pf .-signature fmtpf-entry)]))

(defrecord ^:private JupMsgPreframes
    [envelope delimiter signature]
  Object
  (toString [pf] (fmtpf pf)))

(alter-meta! #'->JupMsgPreframes assoc :private true)

(defn- make-jupmsg-preframes
  [envelope delimiter signature]
  (->JupMsgPreframes envelope delimiter signature))

(s/fdef make-jupmsg-preframes
  :args (s/cat :envelope ::sp/byte-arrays
               :delimiter ::sp/byte-array
               :signature ::sp/byte-array))
(instrument `make-jupmsg-preframes)

(u/define-simple-record-print JupMsgPreframes fmtpf)

;;; ------------------------------------------------------------------------------------------------------------------------
;;; JupMsgBuffers
;;; ------------------------------------------------------------------------------------------------------------------------

(defn- prbufs
  [rec]
  (str "#Buffers" (mapv count (.-buffers rec)) ""))

(defrecord ^:private JupMsgBuffers
    [buffers]
  Object
  (toString [b] (prbufs b)))

(defn- make-jupmsg-buffers
  [buffers]
  (->JupMsgBuffers buffers))

(s/fdef make-jupmsg-buffers
  :args (s/cat :buffers ::sp/byte-arrays))
(instrument `make-jupmsg-buffers)

(alter-meta! #'->JupMsgBuffers assoc :private true)

(u/define-simple-record-print JupMsgBuffers prbufs)

;;; ------------------------------------------------------------------------------------------------------------------------
;;; MAKE-JUPMSG
;;; ------------------------------------------------------------------------------------------------------------------------

(defn make-jupmsg-header
  [message-id msgtype username session date version]
  (->> {:msg_id message-id,
        :msg_type msgtype,
        :username username,
        :session session
        :date date
        :version version}
       (s/assert ::jsp/header)))

(defn make-jupmsg
  [envelope signature header parent-header metadata content buffers]
  (let [delimiter u/IDSMSG-BYTES
        preframes (make-jupmsg-preframes envelope delimiter signature)
        buffers (->JupMsgBuffers buffers)]
    (->> {:header header, :parent-header parent-header, :metadata metadata
          :content content, :preframes preframes, :buffers buffers}
         (s/assert ::jsp/jupmsg))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; BUFFER EXTRACTION AND INSERTION
;;; ------------------------------------------------------------------------------------------------------------------------

;;; Klaus Harbo 2019-12-18: The problem of extracting and re-inserting values would probably be
;;; handled more generally, correctly and succinctly by Specter (cf.
;;; https://github.com/redplanetlabs/specter).  Specter does, however, employ a complex syntax to
;;; express its solutions; it does not seem worthwhile to introduce dependency and deal with the
;;; associated learning curve to solve the buffer extraction and re-insertion problem which can be
;;; adequately solved by the fairly straightforward code in `leaf-paths` and `insert-paths` below.

(defn leaf-paths
  "Recursively traverses `v` replacing elements for which `pred` returns a truthy value with the
  result of applying `f` to the elements.  Returns a 2-tuple of the result of the replacements and a
  map of paths to the replaced elements.

  NOTE: The implementation does NOT work correctly for any Clojure value; it is specialised to
  handle Jupyter messages which are losslessly serializable to JSON.  Extraction only occurs from
  vectors and maps."
  [pred f v]
  (let [T (atom [])]
    (letfn [(inner [path]
              (fn [idx v]
                (let [path' (conj path idx)]
                  (cond
                    (pred v)
                    ,, (do (swap! T conj [(-> path' rest vec) v]) ;; :dummy removed by `rest`
                           (f v))
                    (vector? v)
                    ,, (mapv (inner path') (range) v)
                    (map? v)
                    ,, (reduce-kv (fn [Σ k v] (assoc Σ k ((inner path') k v))) {} v)
                    :else
                    ,, v))))]
      [((inner []) :dummy v)
       (into {} @T)])))

(defn insert-paths
  "Returns the result of inserting into `jupyter-message` values from vals in `path-value-map` at the
  point specified by their keys.  `path-value-map` must be a map from paths to values and all paths
  must refer to insertion points in either maps or vectors.

  Example:

    (let [pred #(and (int? %) (odd? %))
          replfn (constantly :replaced)
          value {:a 1, :b [0 1 [1 2 3 {:x [1]}]]}
          [result paths] (leaf-paths pred replfn value)]
      [(= value (insert-paths result paths)) ;; <-- The point of `leaf-paths` and `insert-paths`
       result paths])
    ;; =>
    [true
     {:a :replaced, :b [0 :replaced [:replaced 2 :replaced {:x [:replaced]}]]}
     {[:a] 1, [:b 1] 1, [:b 2 0] 1, [:b 2 2] 3, [:b 2 3 :x 0] 1}]

  NOTE: The implementation does NOT work correctly for any Clojure value; it is specialised to
  handle Jupyter messages which are losslessly serializable to JSON.  Paths can only refer to
  insertion points in vectors and maps."
  [jupyter-message path-value-map]
  (letfn [(insert-by-path [jupyter-message [k & ks] v]
            (if (or (map? jupyter-message) (vector? jupyter-message))
              (assoc jupyter-message k (if (seq ks)
                                         (insert-by-path (get jupyter-message k) ks v)
                                         v))
              jupyter-message))]
    (reduce (fn [Σ [path v]] (insert-by-path Σ path v))
            jupyter-message path-value-map)))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; FRAMES <-> JUPMSG
;;; ------------------------------------------------------------------------------------------------------------------------

(defn- unkeywordize-keys
  [m]
  (reduce-kv (fn [S k v] (assoc S (name k) v))
             {} m))

(defn- parse-json-and-build-jupmsg
  [raw-map]
  (let [res (fmap (C u/parse-json-str walk/keywordize-keys) raw-map)]
    (if (= (message-msg-type res) COMM-INFO-REPLY)
      (update-in res [:content :comms] unkeywordize-keys)
      res)))

(let [env-delim (vec (take 2 u/SEGMENT-ORDER))
      body-keys (vec (drop 2 u/SEGMENT-ORDER))
      _ (assert (= env-delim [:envelope :delimiter]))
      _ (assert (= body-keys [:signature :header :parent-header :metadata :content]))]

  (defn frames->jupmsg
    "Returns a map representing a Jupyter message.  `frames` must be a sequential collection of
  byte-arrays consisting of a "
    [check-signature frames]
    (let [delim u/IDSMSG-BYTES
          envelope (vec (take-while (complement u/delimiter-frame?) frames))
          _ (assert (< (count envelope) (count frames))) ;; we require a delimiter frame
          blobs (drop (inc (count envelope)) frames)     ;; we're dropping the delimiter too
          n-blobs (count body-keys)
          _ (assert (>= (count blobs) n-blobs))
          body (zipmap body-keys (take n-blobs blobs))
          signature (get body :signature) ; we want signature as bytes
          strbody (fmap u/bytes->string body)
          buffers (make-jupmsg-buffers (vec (drop n-blobs blobs)))
          preframes (make-jupmsg-preframes envelope delim signature)
          orig-body (select-keys strbody [:header :parent-header :metadata :content])]
          ;; BUG: The body-map is deserialized at this point and parsed to transform its keys from strings to keywords
          ;; It is later applied to check-signature fn that will reserialize it (print it to json string) to check its signature.
          ;; Clojure maps don't guarantee position in the printed string, so a single map can generate multiple signatures.
          ;; clojure.walk/keywordize-keys that's used in the parsing fn bellow will change the printing order of deeply nested
          ;; maps and that breaks the signing step.
          ;; We should at this point verify the message signature and continue to parse it only if signature is valid.
      (when-not (check-signature (assoc orig-body :preframes preframes))
        (let [msg "Invalid message signature"]
          (log/debug msg ": " (String. signature "UTF-8") ". jupmsg: " orig-body)
          (log/error msg)
         (throw (Exception. msg))))
      (s/assert ::jsp/jupmsg (assoc (parse-json-and-build-jupmsg orig-body) :preframes preframes :buffers buffers)))))

(s/fdef frames->jupmsg
  :args (s/cat :checker fn?, :frames ::msp/frames)
  :ret ::jsp/jupmsg)
(instrument `frames->jupmsg)

(let [;; don't want envelope+delimiter in sig scope:
      jupmsg-keys (drop 2 u/SEGMENT-ORDER)]

  (defn jupmsg->frames
    [signer {:keys [header parent-header metadata content preframes buffers] :as jupmsg}]
    (assert (every? (complement nil?) (vals (select-keys jupmsg jupmsg-keys))))
    (let [envelope	(.-envelope preframes)
      ;;    signature	(.-signature preframes)
          _		(assert envelope)
          payload-vec	(mapv u/json-str [header parent-header metadata content])
          signature (signer payload-vec)
          _		(assert signature)
          byte-buffers	(when buffers
                          (.-buffers buffers))]
        (assert (s/valid? ::sp/byte-arrays envelope))
        (->> (concat envelope
                     [u/IDSMSG-BYTES (u/string->bytes signature)]
                     (mapv u/string->bytes payload-vec)
                     byte-buffers)
             vec
             (s/assert ::msp/frames)))))

(s/fdef jupmsg->frames
  :args (s/cat :signer fn? :jupmsg ::jsp/jupmsg)
  :ret ::msp/frames)
(instrument `jupmsg->frames)

;;; ------------------------------------------------------------------------------------------------------------------------
;;; JUPMSG <-> KERNEL REQ/RSP
;;; ------------------------------------------------------------------------------------------------------------------------

(defn jupmsg->kernelreq
  [req-port req-message]
  {:error? false, :req-port req-port, :req-message req-message})

(defn transducer-error
  [req-port]
  {:error? true, :req-port req-port})

(defn kernelrsp->jupmsg
  ([port kernel-rsp]
   (kernelrsp->jupmsg port kernel-rsp {}))
  ([port
    {:keys [rsp-content rsp-msgtype rsp-socket rsp-metadata rsp-buffers req-message]}
    {:keys [messageid now] :as opts}]
   (let [messageid	(str (or messageid (u!/uuid)))
         now		(or now (u!/now))
         session-id	(message-session req-message)
         username	(message-username req-message)
         header		(make-jupmsg-header messageid rsp-msgtype username session-id now PROTOCOL-VERSION)
         parent-header	(message-header req-message)
         metadata	(or rsp-metadata {})
         rsp-buffers	(or rsp-buffers [])
         envelope	(if (= rsp-socket :iopub_port)
                          [(u/string->bytes rsp-msgtype)]
                          (message-envelope req-message))
         signature	(u/string->bytes "")]
     (make-jupmsg envelope signature header parent-header metadata rsp-content rsp-buffers))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; MESSAGE CONTENT BUILDERS
;;; ------------------------------------------------------------------------------------------------------------------------

(sdefn clear-output-content
  (s/cat :wait ::wait)
  [wait?]
  {:wait (boolean wait?)})

(sdefn comm-close-content
  (s/cat :comm-id ::uuid, :data ::data)
  [comm-id data]
  {:comm_id comm-id, :data data})

(sdefn comm-info-reply-content
  (s/cat :comm-id-target-name-map (s/map-of ::comm-id ::target_name))
  [comm-id-target-name-map]
  {:status "ok" :comms (->> (for [[comm-id target-name] comm-id-target-name-map]
                              [comm-id {:target_name target-name}])
                            (into {}))})

(sdefn comm-info-request-content
  (s/cat :target-name ::target_name)
  [target-name]
  {:target_name target-name})

(sdefn comm-msg-content
  (s/cat :comm-id ::comm-id, :data ::data, :opts (s/? (s/keys :opt-un [::target_module ::target_name])))
  ([comm-id data]
   (comm-msg-content comm-id data {}))
  ([comm-id data {:keys [target_name target_module]}]
   (merge {:comm_id comm-id, :data data}
          (when target_name
            {:target_name target_name})
          (when target_module
            {:target_module target_module}))))

(sdefn comm-open-content
  (s/cat :comm-id ::comm-id, :data ::data, :opts (s/? (s/keys :opt-un [::target_module ::target_name])))
  ([comm-id data]
   (comm-open-content comm-id data {}))
  ([comm-id data opts]
   ;; `comm-open` and `comm` have identical content structure:
   (comm-msg-content comm-id data opts)))

(sdefn complete-reply-content
  (s/cat :matches ::matches, :start ::cursor-start, :end ::cursor-end)
  [matches cursor-start cursor-end]
  {:matches matches
   :metadata {}
   :cursor_start cursor-start
   :cursor_end cursor-end
   :status "ok"})

(sdefn complete-request-content
  (s/cat :code ::code, :cursor-pos ::cursor-pos)
  [code cursor-pos]
  {:code code, :cursor_pos cursor-pos})

(sdefn display-data-content
  (s/cat :data map?, :metadata map?, :transient map?)
  [data metadata transients-map]
  {:data data, :metadata metadata, :transient transients-map})

(sdefn error-message-content
  (s/cat :exe-count ::msp/execution_count)
  [execution-count]
  {:execution_count execution-count})

(sdefn execute-input-msg-content
  (s/cat :exe-count ::nonneg-int, :code ::code)
  [exe-count code]
  {:execution_count exe-count :code code})

(sdefn execute-reply-content
  (s/cat :status ::status, :exe-count ::nonneg-int :opts (s/? map?))
  ([status execution-count] (execute-reply-content status execution-count {}))
  ([status execution-count {:keys [ename evalue traceback] :as opts}]
   (let [ename (or ename "Error name not available.")
         evalue (or evalue "")
         traceback (or traceback ["(no stacktrace)"])]
     (merge {:status status, :execution_count execution-count}
            (case status
              "ok"	{:user_expressions {}}
              "error"	(merge {:ename ename, :evalue evalue}
                               (when (stacktrace/printing-stacktraces?)
                                 {:traceback traceback}))
              (throw (ex-info (str "execute-reply-content - unknown status: " status)
                       {:opts opts})))))))

(sdefn execute-request-content
  (s/cat :code string?, :allow-stdin? boolean, :silent? boolean?,
         :stop-on-error? boolean, :store-history? boolean?)
  [code allow-stdin? silent? stop-on-error? store-history?]
  {:code code,
   :silent (boolean silent?)
   :store_history (boolean store-history?)
   :user_expressions {}
   :allow_stdin (boolean allow-stdin?)
   :stop_on_error (boolean stop-on-error?)})

(sdefn execute-result-content
  (s/cat :data ::data, :exe-count ::nonneg-int, :opts (s/? map?))
  ([data execution-count] (execute-result-content data execution-count {}))
  ([data execution-count {:keys [metadata]}]
   (let [metadata (or metadata {})]
     {:execution_count execution-count
      :data data
      :metadata metadata})))

(sdefn history-reply-content
  (s/cat :hist-map map?)
  [history-maps]
  {:status "ok", :history (map (juxt :session :line :source) history-maps)})

(sdefn history-request-content
  (s/cat :opts (s/? map?))
  ([] (history-request-content {}))
  ([{:keys [output? raw? hist-access-type session start stop pattern unique?]}]
   (let [output? (or output? true)
         raw? (or raw? false)
         hist-access-type (or hist-access-type "search")
         session (or session 1)
         start (or start 1)
         stop (or stop 1)
         pattern (or pattern "")
         unique? (or unique? true)]
     {:output output?
      :raw raw?
      :hist_access_type hist-access-type
      :session session
      :start start
      :stop stop
      :pattern pattern
      :unique unique?})))

(sdefn input-reply-content
  (s/cat :value ::msp/value)
  [val]
  {:value val})

(sdefn input-request-content
  (s/cat :prompt ::prompt :password (s/? string?))
  ([prompt]
   (input-request-content prompt false))
  ([prompt password?]
   {:prompt (str prompt)
    :password (boolean password?)}))

(sdefn inspect-reply-content
  (s/cat :code ::code, :result-str string?)
  [code result-str]
  (let [found? (not (str/blank? (str result-str)))]
    {:status "ok", :found found?, :code code, :metadata {},
     :data (if found?
             {:text/html (str "<pre>" result-str "</pre>"), :text/plain (str result-str)}
             {})}))

(sdefn inspect-request-content
  (s/cat :code ::code, :cursor-pos ::cursor-pos :opts map?)
  ([code cursor-pos] (inspect-request-content code cursor-pos {}))
  ([code cursor-pos {:keys [details?]}]
   (merge {:code code, :cursor_pos cursor-pos}
          (when details? {:detail_level 1}))))

(defn interrupt-reply-content
  []
  {})

(defn interrupt-request-content
  []
  {})

(sdefn is-complete-reply-content
  (s/cat :status ::status)
  [status]
  {:status status})

(sdefn is-complete-request-content
  (s/cat :code ::code)
  [code]
  {:code (str code)})

(sdefn kernel-info-reply-content
  (s/cat :opts (s/? map?))
  ([protocol-version] (kernel-info-reply-content protocol-version {}))
  ([protocol-version {:keys [banner clj-ver help-links implementation version-string]}]
   (let [version-string		(or version-string clojupyter/version)
         banner			(or banner (str "Clojupyter (" version-string ")"))
         clj-ver		(or clj-ver (clojure-version))
         help-links		(or help-links [])
         implementation		(or implementation "clojupyter")]
     {:status "ok"
      :protocol_version (str protocol-version)
      :implementation (str implementation)
      :implementation_version (str version-string)
      :language_info {:name "clojure"
                      :version (str clj-ver)
                      :mimetype "text/x-clojure"
                      :file_extension ".clj"}
      :banner (str banner)
      :help_links help-links})))

(sdefn kernel-info-request-content
  (s/cat)
  []
  {})

(declare update-comm-msg)
(sdefn output-set-msgid-content
  (s/cat :comm-id ::comm-id :msgid string?)
  [comm-id msgid]
  (let [state {:msg_id msgid}]
    (comm-msg-content comm-id state)))

(sdefn output-update-content
  (s/cat :comm-id ::comm-id
         :target-name ::target_name
         :stream-name #{"stdout" "stderr"}
         :strings (s/coll-of string? :kind vector?))
  [comm-id method target-name stream-name strings]
  (let [state {:outputs (vec (for [s strings]
                               {:name stream-name,
                                :text s,
                                :output_type "stream"}))}]
    (update-comm-msg comm-id method target-name state)))

(sdefn shutdown-reply-content
  (s/cat :restart? boolean?)
  [restart?]
  {:restart (boolean restart?) :status "ok"})

(sdefn shutdown-request-content
  (s/cat :restart? (s/? boolean?))
  ([] (shutdown-request-content false))
  ([restart?]
   {:restart (boolean restart?)}))

(sdefn status-message-content
  (s/cat :state ::msp/execution-state)
  [execution-state]
  {:execution_state execution-state})

(sdefn stream-message-content
  (s/cat :stream-name string?, :text string?)
  [stream-name s]
  {:name stream-name, :text s})

(sdefn update-comm-msg
  (s/cat :comm-id ::comm-id, :method ::msp/comm-message-method,
         :target-name ::msp/target_name,
         :state map?, :buffer-paths (s/? vector?))
  ([comm-id method target-name state]
   (update-comm-msg comm-id method target-name state []))
  ([comm-id method target-name state buffer-paths]
   (comm-msg-content comm-id {:method method, :state state, :buffer_paths buffer-paths}
                     {:target_name target-name})))

(sdefn update-display-data
  (s/cat :data ::data, :metadata ::metadata, :transient ::transient)
  [data metadata tsient]
  {:data data, :metadata metadata, :transient tsient})

(sdefn custom-comm-msg
  (s/cat :comm-id ::comm-id, :method ::msp/comm-message-method,
       :target-name ::msp/target_name,
       :content map?)
  [comm-id method target-name content]
  (comm-msg-content comm-id {:method method :content content} {:target_name target-name}))
