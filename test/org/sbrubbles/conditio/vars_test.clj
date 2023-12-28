(ns org.sbrubbles.conditio.vars-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio.vars :as v])
  (:import (clojure.lang ExceptionInfo)))

(deftest abort
  (is (thrown-with-msg? ExceptionInfo #"abort" (v/abort "abort")))

  (try
    (v/abort "msg" 1 2 3)
    (is false)
    (catch ExceptionInfo e
      (is (= (:args (ex-data e))
             '(1 2 3)))
      (is (= (ex-message e)
             "msg")))))

(deftest skip
  (is (= (v/skip) (v/skip)))

  (is (= (v/skip "a" "b" "c")
         (v/skip 1 2 3)
         (v/skip))))

(deftest initial-handlers
  (is (contains? v/*handlers* #'v/handler-not-found)))

(v/defcondition c)
(v/defcondition c-with-doc "doc")
(v/defcondition c-with-meta {:sbrubbles 1 :doc "sbrubbles"})
(v/defcondition c-with-obj 42)

(deftest defcondition-metadata
  (are [v field val] (= (field (meta v)) val)
       #'c :doc nil
       #'c :sbrubbles nil
       #'c :metadata nil

       #'c-with-doc :doc "doc"
       #'c-with-doc :sbrubbles nil
       #'c-with-doc :metadata nil

       #'c-with-meta :doc "sbrubbles"
       #'c-with-meta :sbrubbles 1
       #'c-with-meta :metadata nil

       #'c-with-obj :doc nil
       #'c-with-obj :sbrubbles nil
       #'c-with-obj :metadata 42))


(deftest handling
  (is (thrown? ExceptionInfo (c 1 2 3)))

  (testing "calling c is the same as calling v/signal"
    (v/handle [c list]
      (is (= (v/*handlers* #'c)
             (list list)))

      (is (= (v/signal #'c 1 2 3)
             (c 1 2 3)
             (list 1 2 3)))))

  (testing "v/handle-fn and v/handle"
    (let [c-prime (v/handle-fn {#'c list}
                               (fn [& args]
                                 (apply c args)))]
      (is (= (c-prime 1 2 3)
             (list 1 2 3))))

    (v/handle [c v/abort]
      (let [c-prime (v/handle-fn {#'c list}
                                 (fn [& args]
                                   (apply c args)))]
        (is (= (c-prime 1 2 3)
               (list 1 2 3))))))

  (is (thrown? ExceptionInfo (c 1 2 3))))

(def ^:dynamic *r* (partial v/abort "No restart defined!"))

(deftest handle-with-restarts
  (v/handle [c #(*r* %)]
    (binding [*r* inc]
      (is (= (c 1) 2))))

  (binding [*r* inc]
    (v/handle [c #(*r* %)]
      (is (= (c 1) 2)))))

(deftest skipping
  (testing "conditional skipping"
    (v/handle [c inc]
      (v/handle [c #(if (even? %)
                      :even
                      (v/skip))]
        (is (= (c 1) 2))
        (is (= (c 2) :even)))))

  (testing "skip all handlers"
    (v/handle [c v/skip]
      (is (thrown? ExceptionInfo (c 1 2 3))))))
