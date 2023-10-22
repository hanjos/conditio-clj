(ns org.sbrubbles.conditio.vars-test.examples.parse-log-entry-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio.vars :as v]))


;; fixtures
(v/defcondition *malformed-log-entry*)

(v/defrestart *skip-entry*)

(defn parse-log-entry [line]
  (if (not= line :fail)
    (str ">>> " line)
    (*malformed-log-entry* line)))

(defn parse-log-file []
  (comp (map (v/with-fn {#'*skip-entry* (fn [& _] ::skip-entry)}
                        parse-log-entry))
        (filter #(not= % ::skip-entry))))

(defn analyze-logs [args]
  (binding [*malformed-log-entry* *skip-entry*]
    (into []
          (comp cat
                (parse-log-file))
          args)))

;; tests