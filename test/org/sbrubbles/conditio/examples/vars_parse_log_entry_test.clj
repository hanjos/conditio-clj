(ns org.sbrubbles.conditio.examples.vars-parse-log-entry-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio.vars :as v])
  (:import
    (clojure.lang ExceptionInfo)))

(v/defcondition malformed-log-entry)
(v/defrestart retry-with)
(v/defrestart skip-entry)

(def ^:dynamic *selected-handler* skip-entry)

;; functions

(defn parse-log-entry [line]
  (if (not (= line :fail))
    (str ">>> " line)
    (v/with [retry-with parse-log-entry]
      (malformed-log-entry line))))

(defn parse-log-file []
  (comp (map (v/with-fn {#'skip-entry (fn [] ::skip-entry)}
                        parse-log-entry))
        (filter #(not (= % ::skip-entry)))))

(defn analyze-logs [& args]
  (v/handle [malformed-log-entry *selected-handler*]
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
       (select-handler analyze-logs (fn [_] (skip-entry)))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> d" ">>> e"]

       ; :fail's are replaced with "X", no parsing
       (select-handler analyze-logs (fn [_] "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" "X" "X" "X" ">>> d" "X" ">>> e"]

       ; :fail's are reparsed with "X" as input instead
       (select-handler analyze-logs (fn [_] (retry-with "X")))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> X" ">>> X" ">>> X" ">>> d" ">>> X" ">>> e"]))

(deftest abort-analyze
  (binding [*selected-handler* v/abort]
    (is (thrown? ExceptionInfo
                 (analyze-logs ["a" :fail])))))
