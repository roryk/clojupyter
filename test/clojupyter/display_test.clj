(ns clojupyter.display-test
  (:require
   [cheshire.core				:as cheshire]
   [midje.sweet							:refer [facts =>]]
   ,,
   [clojupyter.display			:as display]
   [clojupyter.misc.mime-convertible]
   [clojupyter.test-shared			:as ts]
   [clojupyter.protocol.mime-convertible	:as mc]
   ))

(defn pretty-maker [maker]
  #(-> % maker mc/to-mime (cheshire/decode true)))

(facts "Should be able to produce html"
        ((pretty-maker display/html) "<h1>hello world</h1>") => {:text/html "<h1>hello world</h1>"})

(facts "Should be able to produce latex"
        ((pretty-maker display/latex) "$\\sin{x}$") => {:text/latex "$\\sin{x}$"})

(facts "Should be able to produce markdown"
        ((pretty-maker display/markdown) "# hello world") => {:text/markdown "# hello world"})

(facts "Should be able to produce from hiccup html"
        ((pretty-maker display/hiccup) [:p "some text"]) => {:text/html "<p>some text</p>"})

(facts "Should be able to produce rich content"
        (.to-mime (display/render-mime :foo/bar "Foobar")) => "{\"foo/bar\":\"Foobar\"}")
