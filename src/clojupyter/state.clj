(ns clojupyter.state
  (:require [clojupyter.kernel.comm-global-state :as comm-global-state]
            [clojupyter.kernel.history :as his]
            [clojupyter.util-actions :as u!]
            [clojupyter.zmq-util :as zutil]
            [io.simplect.compose :refer [p P]]))

(def ^:private EMPTY-QUEUE [])

(def STATE (atom nil))

(defrecord State [execute-count display-queue history-session zmq-context halt?
                  comms cur-ctx])

(u!/set-var-private! #'->State)

(defn- make-state
  ([] (make-state nil))
  ([{:keys [:zmq-context]}]
   ;; 2019-11-15 Klaus Harbo:
   ;; If state is reinitialized, we keep zmq-context because JeroMQ expects
   ;; a single context for the entire lifetime of the (OS) process
   (let [execute-count	1N
         display-queue	EMPTY-QUEUE
         sess		(-> (his/init-history) his/start-history-session)
         zmq-context	(or zmq-context (zutil/zcontext))
         halt?		false
         comms		(comm-global-state/initial-state)
         cur-ctx	()]
     (->State execute-count display-queue sess zmq-context halt? comms cur-ctx))))

(defn ensure-initial-state!
  ([]
   (ensure-initial-state! false))
  ([force?]
   (when (or force? (not @STATE))
     (reset! STATE (make-state @STATE)))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; EXECUTE-COUNT
;;; ------------------------------------------------------------------------------------------------------------------------

(defn inc-execute-count!
  "Increments Jupyter Execution Counter by 1."
  []
  (swap! STATE (P update-in [:execute-count] inc)))

(defn execute-count
  "Returns current value of Jupyter Execution Counter."
  []
  (:execute-count @STATE))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; ZEROMQ
;;; ------------------------------------------------------------------------------------------------------------------------

(defn zmq-context
  []
  (:zmq-context @STATE))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; COMMS
;;; ------------------------------------------------------------------------------------------------------------------------

(defn comm-state-get
  "Returns the overall COMM state."
  []
  (:comms @STATE))

(defn comm-state-swap!
  "Updates overall COMM state to be the result of applying `f` to the current COMM state."
  [f]
  (swap! STATE (P update-in [:comms] f)))


;;; ------------------------------------------------------------------------------------------------------------------------
;;; CURRENT-CONTEXT
;;; ------------------------------------------------------------------------------------------------------------------------

(defn push-context!
  "Pushes `ctx` to global context stack. Returns `ctx`.

  Note: `with-current-context` is the recommended way to managing the context stack."
  ;;
  ;; 2019-12-05 Klaus Harbo
  ;;
  ;; We need this because Clojupyter functions called by user code (in Jupyter cells) -
  ;; e.g. widget-creating functions - need context to generate the right side-effects. Setting a
  ;; value in the global state is necessary because user code is evaluated on a separate thread
  ;; managed by NREPL to which we have a TCP connection and thus effectively communicate with using
  ;; text.
  ;;
  ;; The `core.async` channels used to communicate with Jupyter cannot be encoded and sent to the
  ;; NREPL thread and it seems like overkill to implement a protocol enabling the two to enchange
  ;; the data needed for the NREPL client to perform actions on behalf of the Clojupyter functions
  ;; called by user code.
  ;;
  ;; It would be desirable to avoid this global state but as of now it does not seem worthwhile to
  ;; invest a lot in avoiding it as it seems that the interaction model between Jupyter and kernels
  ;; require strict serialization anyway because the client (Lab, Notebook, ...) needs `busy`/`idle`
  ;; signals to know when they have seen all updates.  So we must finish handling one request
  ;; (EXECUTE-REQUEST, COMM-*, or one of the others) before we can start processing the next one
  ;; anyway.  The notion of a 'current request' fits this model just fine.
  ;;
  ;; So, at least for now, this is how we do it.
  ;;
  [ctx]
  (swap! STATE (P update :cur-ctx (p cons ctx)))
  ctx)

(defn current-context
  []
  (first (:cur-ctx @STATE)))

(defn swap-context!
  [f]
  (swap! STATE update :cur-ctx (fn [ctx-list] (cons (f (first ctx-list)) (rest ctx-list)))))

(defn pop-context!
  "Pops top of context stack and returns it.  Throws Exception if stack is empty.

  Note: `with-current-context` is the recommended way to managing the context stack."
  []
  (if-let [ctx (current-context)]
    (do (swap! STATE (P update :cur-ctx rest))
        ctx)
    (throw (ex-info "pop-context! - empty stack" {:STATE @STATE}))))

(defmacro with-current-context
  [[ctx] & body]
  `(try (push-context! ~ctx)
        ~@body
        (finally (pop-context!))))

;;; ------------------------------------------------------------------------------------------------------------------------
;;; HISTORY
;;; ------------------------------------------------------------------------------------------------------------------------

(defn add-history!
  [code]
  (let [sess (:history-session @STATE)
        exe-count (:execute-count @STATE)]
    (assert sess "Clojupyter internal error: History session not found.")
    (assert exe-count "Clojupyter internal error: Execute count not found.")
    (his/add-history sess exe-count code)))

(defn get-history
  []
  (let [sess (:history-session @STATE)]
    (assert sess "Clojupyter internal error: History session not found.")
    (his/get-history sess)))

(defn end-history-session
  "Returns the history session."
  []
  (his/end-history-session (:history-session @STATE)))
