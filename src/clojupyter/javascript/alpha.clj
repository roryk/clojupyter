(ns clojupyter.javascript.alpha
  "Provides access to third party Javascript libraries in clojupyter.

  Replaces `clojupyter.misc.helpter/add-javascript` which is
  deprecated because Jupyter no longer allows arbitrary `script` tags
  to be sent by kernels but instead supports libraries loaded using
  RequireJS (cf. https://requirejs.org) an implementation of
  Javascript Asynchronuous Module Definitions (AMDs).

  Alpha version - breakage may happen.  Please report issues or
  missing features."
  (:require [clojupyter.display :as display]
            [clojupyter.util-actions :as u!]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [clojure.string :as str]))

;;; ----------------------------------------------------------------------------------------------------
;;; REQUIREJS - ASYNCHRONUOUS MODULE DEFINITIONS (AMDs)
;;; ----------------------------------------------------------------------------------------------------

(s/def ::ident		keyword?)
(s/def ::exports	string?)
(s/def ::url		(s/and string? (partial re-find #"^https?://")))
(s/def ::jsdef		(s/keys :req-un [::ident ::exports ::url]))
(s/def ::jsdefs		(s/coll-of ::jsdef :kind vector?))

(defn amd-wrap-config
  "Returns a string represeting the call if `requirejs.config()` with
  arguments resulting from converting `s` to a string."
  [s]
  (str "requirejs.config(" s ")"))

(defn amd-wrap-semicolons
  "Returns a string by concatenating the members of `ss` after having
  appended \\; to each value."
  [& ss]
  (str (str/join ";" ss) ";"))

(defn amd-wrap-require
  "Returns a string representing a Javascript RequireJS `require()` statement.
  Cf. `amd-add-javascript` for usage information."
  [ident-vec javascript-function]
  (amd-wrap-semicolons
   (format "require(%s,%s)" (json/generate-string (mapv name ident-vec)) javascript-function)))

(defn amd-add-javascript
  "Returns a string representing a Javascript `config()` statement for
  RequireJS (cf. https://requirejs.org) providing access to Javascript
  libraries using Asynchronuous Module Definitions (AMDs).

  `jsdefs` must be a vector of `jsdef` (cf. `clojure.spec` definition
  `::jsdef` in namespace `clojupyter.javascript-amd`).

  Example:

   c.j.alpha> (println
               (amd-add-javascript
                [{:ident :hicharts
                  :exports \"Highcharts\"
                  :url \"https://code.highcharts.com/highcharts\"}]))
   requirejs.config({\"paths\":{\"hicharts\":\"https:\\/\\/code.highcharts.com\\/highcharts\"},
   \"shim\":{\"hicharts\":{\"exports\":\"Highcharts\"}}})
   nil
   c.j.alpha>

  (Printed line above broken for readability.)

  The above expression yields a RequireJS configuration providing
  access to the Highcharts Javascript library available at
  `code.highcharts.com` and referenced using `:hicharts` using
  `wrap-require`.  The library exports the symbol `Highcharts`.

  Given the above RequireJS configuration, the Highcharts library can
  be accessed using `wrap-require`.

  Example:

  c.j.alpha> (let [nodeid (gensym), data {:a 1}]
               (println
                (amd-wrap-require [:hicharts]
                   (format \"function(hc){hc.chart('%s',%s)}\" nodeid (json/write-str data)))))
  require([\"hicharts\"],function(hc){hc.chart('G__34952',{\"a\":1})});
  nil
  c.j.alpha>

  where the function's formal parameter `hc` is bound to the exported
  symbol `Highcharts` from the Highchart library which is referenced
  by `:hicharts`.  The number of ident-bindings in `wrap-require` and
  the number of arguments in the Javascript function must be the same,
  and referenced symbols are bound in order.

  NOTE: The result of calling `amd-add-javascript` must be the last
  form of the cell for the Javascript to be evaluated.  Consider using
  `amd-add-javascript-html` which creates a HTML `div` element which
  both add the Javascript library and provides a textual
  confirmation."
  [jsdefs]
  (-> {:paths (->> jsdefs
                   (map (juxt :ident :url))
                   (into {}))
       :shim (->> jsdefs
                  (map (fn [{:keys [ident exports]}] {ident {:exports exports}}))
                  (into {}))}
      json/generate-string
      amd-wrap-config))

(defn amd-add-javascript-html
  "Same as `amd-add-javascript` except the returned string is embedded in a
  `script` element wrapped in a `hiccup` form.
  Cf. `amd-add-javascript` for details.

  NOTE: Evaluation of `amd-add-javascript-html` must be the last form
  of the cell.  The message 'Javascript library loaded...' indicates
  that the library was loaded."
  ([jsdefs] (amd-add-javascript-html {} jsdefs))
  ([{:keys [brief?] :or {brief? false}} jsdefs]
   (display/hiccup [:div
                         [:script (amd-wrap-semicolons (amd-add-javascript jsdefs))]
                         (if brief?
                           [:pre "Javascript libraries added."]
                           (if (= (count jsdefs) 1)
                             [:pre (str "Javascript library added: " (-> jsdefs first :url) ".")]
                             [:pre (str "Javascript libraries added:\n")
                              (str/join "\n" (map #(str "  - " (:url %)) jsdefs))]))])))

(do
  (s/fdef amd-add-javascript
    :args (s/cat :jsdefs ::jsdefs)
    :ret string?)
  (s/fdef amd-add-javascript-html
    :args (s/cat :opts (s/? map?) :jsdefs ::jsdefs)
    :ret string?)
  (s/fdef amd-wrap-require
    :args (s/cat :ident-vec (s/coll-of ::ident :kind vector?)
                 :javascript-function string?)
    :ret string?)
  (instrument `amd-add-javascript)
  (instrument `amd-add-javascript-html)
  (instrument `and-wrap-require)
  (map (partial u!/assoc-meta! :style/indent :defn)
       [#'amd-wrap-require #'amd-wrap-semicolons #'amd-wrap-config]))
