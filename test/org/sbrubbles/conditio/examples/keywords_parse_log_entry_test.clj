(ns org.sbrubbles.conditio.examples.keywords-parse-log-entry-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio :as c])
  (:import (clojure.lang ExceptionInfo)))

(def ^:dynamic *selected-handler* ::skip-entry)

;; functions

(defn parse-log-entry [line]
  (if (not= line :fail)
    (str ">>> " line)
    (c/with [::retry-with parse-log-entry]
            (c/signal ::malformed-log-entry :line line))))

(defn parse-log-file []
  (comp (map (c/with-fn {::skip-entry (fn [] ::skip-entry)}
                        parse-log-entry))
        (filter #(not= % ::skip-entry))))

(defn analyze-logs [& args]
  (c/handle [::malformed-log-entry *selected-handler*]
    (into []
          (comp cat
                (parse-log-file))
          args)))

;; helper test fixture
(defn select-handler [f handler]
  (binding [*selected-handler* handler]
    (bound-fn* f)))

(deftest analyze-logs-test
  (are [f input expected] (= (apply f input) expected)
       ; everything except :fail is parsed
       (select-handler analyze-logs (fn [_] (c/restart ::skip-entry)))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> d" ">>> e"]

       ; :fail's are replaced with "X", no parsing
       (select-handler analyze-logs (fn [_] "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" "X" "X" "X" ">>> d" "X" ">>> e"]

       ; :fail's are reparsed with "X" as input instead
       (select-handler analyze-logs (fn [_] (c/restart ::retry-with "X")))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> X" ">>> X" ">>> X" ">>> d" ">>> X" ">>> e"]))

(deftest abort-analyze
  (binding [*selected-handler* c/abort]
    (is (thrown-with-msg? ExceptionInfo #"Abort"
                          (analyze-logs ["a" :fail])))))
