(ns org.sbrubbles.conditio.vars-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio.vars :as v])
  (:import (clojure.lang ExceptionInfo)))

;; fixtures
(v/defcondition *no-doc-c*)
(v/defcondition *doc-c* "Docs")
(v/defcondition *map-c* {::key 1})

(v/defrestart *no-doc-r*)
(v/defrestart *doc-r* "Docs")
(v/defrestart *map-r* {::key 2})

(defn ^:dynamic *test-restart* [] :success)

;; tests
(deftest condition-metadata
  (are [v tag value] (= value (tag (meta v)))
       #'*no-doc-c* :dynamic true
       #'*no-doc-c* :doc nil
       #'*no-doc-c* ::key nil

       #'*doc-c* :dynamic true
       #'*doc-c* :doc "Docs"
       #'*doc-c* ::key nil

       #'*map-c* :dynamic true
       #'*map-c* :doc nil
       #'*map-c* ::key 1))

(deftest restart-metadata
  (are [v tag value] (= value (tag (meta v)))
       #'*no-doc-r* :dynamic true
       #'*no-doc-r* :doc nil
       #'*no-doc-r* ::key nil

       #'*doc-r* :dynamic true
       #'*doc-r* :doc "Docs"
       #'*doc-r* ::key nil

       #'*map-r* :dynamic true
       #'*map-r* :doc nil
       #'*map-r* ::key 2))

(deftest condition-explodes-when-called
  (try
    (*no-doc-c* "line")
    (is false "Should never reach here")
    (catch Exception e
      (is (instance? ExceptionInfo e))
      (is (= "*no-doc-c*"
             (ex-message e)))
      (is (= {:args ["line"]}
             (ex-data e))))))

(deftest restart-explodes-when-called
  (try
    (*no-doc-r* "line")
    (is false "Should never reach here")
    (catch Exception e
      (is (instance? ExceptionInfo e))
      (is (= "*restart-not-found*"
             (ex-message e)))
      (is (= {:args '("*no-doc-r*" "line")}
             (ex-data e))))))

(deftest restart-resolves-sym-and-runs-them
  (is (thrown? Exception
               (v/restart 'nonexistent)))

  (is (= :success (v/restart 'org.sbrubbles.conditio.vars-test/*test-restart*))))

(deftest with-fn-adds-restarts
  (let [f (fn [] (v/restart 'org.sbrubbles.conditio.vars-test/*no-doc-r*))
        bound-f (v/with-fn {#'*no-doc-r* (fn [] :success)}
                           f)]
    (is (thrown-with-msg? ExceptionInfo #"\*restart-not-found\*"
                          (f)))

    (binding [*no-doc-r* (fn [] :success)]
      (is (= :success (f))))

    (is (= :success (bound-f)))))
