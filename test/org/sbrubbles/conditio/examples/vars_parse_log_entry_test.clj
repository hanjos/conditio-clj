(ns org.sbrubbles.conditio.examples.vars-parse-log-entry-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio.vars :as v])
  (:import
    (clojure.lang ExceptionInfo)))

(v/defcondition malformed-log-entry)
(def ^:dynamic *retry-with*)
(def ^:dynamic *skip-entry*)

(def ^:dynamic *selected-handler* *skip-entry*)

;; functions

(defn parse-log-entry [line]
  (if (not= line :fail)
    (str ">>> " line)
    (binding [*retry-with* parse-log-entry]
      (malformed-log-entry line))))

(defn parse-log-file []
  (comp (map (v/bind-fn {#'*skip-entry* (fn [] ::skip-entry)}
                        parse-log-entry))
        (filter #(not= % ::skip-entry))))

(defn analyze-logs [& args]
  (v/handle [malformed-log-entry *selected-handler*]
    (into []
          (comp cat
                (parse-log-file))
          args)))

;; helper test fixture
(defn handle-with [f handler]
  (v/bind-fn {#'*selected-handler* handler} f))

(deftest analyze-logs-test
  (are [f input expected] (= (apply f input) expected)
       ; everything except :fail is parsed
       (handle-with analyze-logs (fn [_] (*skip-entry*)))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> d" ">>> e"]

       ; :fail's are replaced with "X", no parsing
       (handle-with analyze-logs (fn [_] "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" "X" "X" "X" ">>> d" "X" ">>> e"]

       ; :fail's are reparsed with "X" as input instead
       (handle-with analyze-logs (fn [_] (*retry-with* "X")))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> X" ">>> X" ">>> X" ">>> d" ">>> X" ">>> e"]))

(deftest abort-analyze
  (binding [*selected-handler* v/abort]
    (is (thrown? ExceptionInfo
                 (analyze-logs ["a" :fail])))))
