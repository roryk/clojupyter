(ns clojupyter.messages-specs
  (:require [clojure.spec.alpha :as s]
            [clojupyter.specs :as sp]
            [clojupyter.util :as u]
            [io.simplect.compose :refer [p C]]))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; SHARED
;;; ------------------------------------------------------------------------------------------------------------------------
(s/def ::nonneg-int			(s/and integer? (complement neg?)))
(s/def ::uuid				string?)
,,
(s/def ::code				string?)
(s/def ::comm_id			::uuid)
(s/def ::comms				(s/map-of ::uuid ::target_map))
(s/def ::data				map?)
(s/def ::execution_count		::nonneg-int)
(s/def ::empty-map			(s/and map? (C count zero?)))
(s/def ::metadata			map?)
(s/def ::name				string?)
(s/def ::reply-message			(s/keys :req-un [::status]))
(s/def ::restart			boolean?)
(s/def ::status				#{"ok" "error" "complete" "incomplete"})
(s/def ::target_module			(s/nilable string?))
(s/def ::target_name			string?)
(s/def ::target_map			(s/keys :req-un [::target_name]
                                                :req-opt [::target_module]))
(s/def ::transient			map?)

;;; ------------------------------------------------------------------------------------------------------------------------
;;; JUPYTER CONFIG
;;; ------------------------------------------------------------------------------------------------------------------------

(s/def ::port				(s/and integer? #(<= 0 % 65535)))

(s/def ::control_port			::port)
(s/def ::shell_port			::port)
(s/def ::stdin_port			::port)
(s/def ::iopub_port			::port)
(s/def ::hb_port			::port)
(s/def ::ip				string?)
(s/def ::key				string?)
(s/def ::transport			(s/and string? (p = "tcp")))
(s/def ::signature_scheme		(s/and string? (p = "hmac-sha256")))
(s/def ::jupyter-config			(s/keys :req-un [::control_port ::shell_port ::stdin_port ::iopub_port ::hb_port
                                                         ::ip ::transport ::key ::signature_scheme]))

(s/def ::socket				#{:control_port :shell_port :stdin_port :iopub_port :hb_port})

;;; ------------------------------------------------------------------------------------------------------------------------
;;; JUPYTER FRAMES
;;; ------------------------------------------------------------------------------------------------------------------------

(defn- delimiter-index
  [^bytes frames]
  (-> (map u/delimiter-frame? frames)
      (#(.indexOf % true))))

(defn- post-delimiter-frame-count
  [frames]
  (- (count frames) (delimiter-index frames) 1))

(s/def ::frames				(s/and ::sp/byte-arrays
                                               (p some u/delimiter-frame?)
                                               (C post-delimiter-frame-count #(>= % 5))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; ---------------------------------------- JUPYTER PROTOCOL MESSAGES CONTENT ---------------------------------------------
;;; ------------------------------------------------------------------------------------------------------------------------

;;; CLEAR-OUTPUT
(s/def ::wait				boolean?)
(s/def ::clear-output-content		(s/keys :req-in [::wait]))

;;; COMM-CLOSE
(s/def ::comm-close-content		(s/keys :req-un [::comm_id, ::data]))

;;; COMM-INFO-REPLY
(s/def ::comm-info-reply-content	(s/merge (s/keys :req-un [::comms]), ::reply-message))

;;; COMM-INFO-REQUEST
(s/def ::comms-map			(s/keys :req-un [::comms]))
(s/def ::comm-info-request-content	::target_map)

;;; COMM-MSG
(s/def ::comm-message-method		#{"update" "request_state" "custom"})
(s/def ::comm-message-content		(s/keys :req-un [::comm_id, ::data]))

;;; COMM-OPEN
(s/def ::comm-open-content		(s/merge (s/keys :req-un [::comm_id, ::data]), ::target_map))

;;; COMPLETE-REPLY
(s/def ::cursor_start			::nonneg-int)
(s/def ::cursor_end			::nonneg-int)
(s/def ::matches			(s/coll-of string? :kind sequential?))
(s/def ::complete-reply-content		(s/merge (s/keys :req-un [::matches, ::cursor_start, ::cursor_end, ::metadata])
                                                 ::reply-message))
;;; COMPLETE-REQUEST
(s/def ::cursor_pos			::nonneg-int)
(s/def ::complete-request-content	(s/keys :req-un [::code, ::cursor_pos]))

;;; DISPLAY-DATA
(s/def ::display-data-content		(s/keys :req-un [::data, ::metadata, ::transient]))

;;; ERROR-MSG
(s/def ::error-message-content		(s/keys :req-un [::execution_count]))

;;; EXECUTE-INPUT
(s/def ::execute-input-content		(s/keys :req-un [::code, ::execution_count]))

;;; EXECUTE-REPLY
(s/def ::execute-reply-content		(s/merge (s/keys :req-un [::execution_count]), ::reply-message))

;;; EXECUTE-REQUEST
(s/def ::silent				boolean?)
(s/def ::store_history			boolean?)
(s/def ::user_expressions		map?)
(s/def ::allow_stdin			boolean?)
(s/def ::stop_on_error			boolean?)
(s/def ::execute-request-content	(s/keys :req-un [::code, ::silent, ::store_history, ::user_expressions,
                                                         ::allow_stdin, ::stop_on_error]))

;;; EXECUTE-RESULT
(s/def ::execute-result-content		(s/keys :req-un [::execution_count, ::data, ::metadata]))

;;; HISTORY-REPLY
(s/def ::history			(s/coll-of (s/tuple int? int? (s/or :string string?,
                                                                            :pair (s/tuple string? string?)))))
(s/def ::history-reply-content		(s/merge (s/keys :req-un [::history]), ::reply-message))

;;; HISTORY-REQUEST
(s/def ::output				boolean?)
(s/def ::raw				boolean?)
(s/def ::hist_access_type		#{"range" "tail" "search"})
(s/def ::session			::nonneg-int)
(s/def ::start				(s/and int? pos?))
(s/def ::stop				(s/and int? pos?))
(s/def ::pattern			string?)
(s/def ::unique				boolean?)
(s/def ::history-request-content	(s/keys :req-opt [::output, ::raw, ::hist_access_type, ::session,
                                                          ::start, ::stop, ::n, ::pattern, ::unique]))

;;; INPUT-REQUEST
(s/def ::prompt				string?)
(s/def ::password			boolean?)
(s/def ::input-request-content		(s/keys :req-un [::prompt], :opt-un [::password]))

;;; INPUT-REPLY
(s/def ::value				string?)

;;; INSPECT-REPLY
(s/def ::inspect-reply-content		(s/merge (s/keys :req-un [::found, ::data, ::metadata]), ::reply-message))

;;; INSPECT-REQUEST
(s/def ::detail_level			#{0 1})
(s/def ::inspect-request-content	(s/keys :req-un [::code, ::cursor_pos], :req-opt [::detail_level]))

;;; INTERRUPT-REPLY
(s/def ::interrupt-reply-content	::empty-map)

;;; INTERRUPT-REQUEST
(s/def ::interrupt-request-content	::empty-map)

;;; IS-COMPLETE-REPLY
(s/def ::indent				string?)
(s/def ::is-complete-reply-content	(s/merge (s/keys :req-opt [::indent]), ::reply-message))

;;; IS-COMPLETE-REQUEST
(s/def ::is-complete-request-content	(s/keys :req-un [::code]))

;;; KERNEL-INFO-REPLY
(s/def ::text				string?)
(s/def ::url				string?)
(s/def ::help_links			(s/coll-of (s/keys :req-un [::text, ::url]) :kind sequential?))
(s/def ::version			string?)
(s/def ::mimetype			string?)
(s/def ::file_extension			string?)
(s/def ::pygments_lexer			string?)
(s/def ::codemirror_mode		(s/or :string string? :map map?))
(s/def ::nbconvert_exporter		string?)
(s/def ::language_info			(s/keys :req-un  [::name, ::version, ::mimetype, ::file_extension]
                                                :req-opt [::pygments_lexer, ::codemirror_mode, ::nbconvert_exporter]))
(s/def ::kernel-info-reply-content	(s/merge (s/keys :req-un  [::protocol_version, ::implementation,
                                                                   ::implementation_version, ::language_info,
                                                                   ::banner]
                                                         :req-opt [::banner, ::help_links])
                                                 ::reply-message))

;;; KERNEL-INFO-REQUEST
(s/def ::kernel-info-request-content	::empty-map)

;;; SHUTDOWN-REPLY
(s/def ::shutdown-reply-content		(s/merge (s/keys :req-un [::restart]), ::reply-message))

;;; SHUTDOWN-REQUEST
(s/def ::shutdown-request-content	(s/keys :req-un [::restart]))

;;; STATUS-MSG
(s/def ::execution-state		#{"busy" "idle" "starting"})
(s/def ::status-message-content		(s/keys :req-un [::execution_state]))

;;; STREAM-MSG
(s/def ::stream-message-content		(s/keys :req-un [::name, ::text]))

;;; UPDATE-DISPLAY-DATA
(s/def ::update-display-data-content	(s/keys :req-un [::data, ::metadata, ::transient]))
