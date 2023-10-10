(ns org.sbrubbles.conditio-test.examples.parse-log-entry-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio :as c]))

(def ^:dynamic *selected-restart* ::skip-entry)

;; functions

(defn parse-log-entry [line]
  (if (not (= line :fail))
    (str "> " line)
    (c/with-restarts [::use-value identity
                      ::retry-with parse-log-entry]
      (c/signal ::malformed-log-entry line))))

(defn parse-log-file []
  (comp (map (c/with-restarts-fn parse-log-entry
                                 {::skip-entry (fn [] ::skip-entry)}))
        (filter #(not (= % ::skip-entry)))))

(defn analyze-logs [& args]
  (c/handle [::malformed-log-entry *selected-restart*]
    (into []
          (comp cat
                (parse-log-file))
          args)))

;; helper test fixture
(defn bind-with-restart [f selected-restart]
  (binding [*selected-restart* selected-restart]
    (bound-fn* f)))

(deftest analyze-logs-test
  (are [f input expected] (= (apply f input) expected)
       (bind-with-restart analyze-logs (c/use-restart ::skip-entry))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       ["> a" "> b" "> c" "> d" "> e"]

       (bind-with-restart analyze-logs (c/use-restart ::use-value "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       ["> a" "> b" "> c" "X" "X" "X" "> d" "X" "> e"]

       (bind-with-restart analyze-logs (c/use-restart ::retry-with "X"))
       [["a" "b"] ["c" :fail :fail] [:fail "d" :fail "e"]]
       ["> a" "> b" "> c" "> X" "> X" "> X" "> d" "> X" "> e"]))


