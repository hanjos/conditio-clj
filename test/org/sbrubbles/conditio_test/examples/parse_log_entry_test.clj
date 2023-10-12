(ns org.sbrubbles.conditio-test.examples.parse-log-entry-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio :as c])
  (:import (clojure.lang ExceptionInfo)))

(def ^:dynamic *selected-restart* ::skip-entry)

;; functions

(defn parse-log-entry [line]
  (if (not (= line :fail))
    (str ">>> " line)
    (c/with [::use-value identity
             ::retry-with parse-log-entry]
            (c/signal ::malformed-log-entry :line line))))

(defn parse-log-file []
  (comp (map (c/with-fn parse-log-entry
                        {::skip-entry (fn [] ::skip-entry)}))
        (filter #(not (= % ::skip-entry)))))

(defn analyze-logs [& args]
  (c/handle [::malformed-log-entry *selected-restart*]
    (into []
          (comp cat
                (parse-log-file))
          args)))

;; helper test fixture
(defn select-restart [f selected-restart]
  (binding [*selected-restart* selected-restart]
    (bound-fn* f)))

(deftest analyze-logs-test
  (are [f input expected] (= (apply f input) expected)
       ; everything except :fail is parsed
       (select-restart analyze-logs (c/restart ::skip-entry))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> d" ">>> e"]

       ; :fail's are replaced with "X", no parsing
       (select-restart analyze-logs (c/restart ::use-value "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" "X" "X" "X" ">>> d" "X" ">>> e"]

       ; :fail's are reparsed with "X" as input instead
       (select-restart analyze-logs (c/restart ::retry-with "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       [">>> a" ">>> b" ">>> c" ">>> X" ">>> X" ">>> X" ">>> d" ">>> X" ">>> e"]))

(deftest abort-analyze
  (let [analyze-log* (select-restart analyze-logs (c/abort "Abort"))]
    (is (thrown-with-msg? ExceptionInfo #"Abort"
                          (analyze-log* ["a" :fail])))))
