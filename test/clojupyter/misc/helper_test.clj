(ns clojupyter.misc.helper-test
  (:require [cheshire.core		    :as cheshire]
            [clojupyter.misc.helper     :as helper]
            [clojupyter.test-shared		:as ts]
            [midje.sweet				:refer [facts =>]]))

(facts
 "add-javascript should generate correct hiccup HTML"
 (cheshire/decode (.to-mime (helper/add-javascript "http://host.com/some-lib.js")) true)
 => {:text/html "<script src=\"http://host.com/some-lib.js\"></script>"})
