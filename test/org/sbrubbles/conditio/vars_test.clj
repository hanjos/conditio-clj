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

(deftest handlers-and-restarts
  (is (contains? v/*handlers* #'v/handler-not-found))
  (is (contains? v/*handlers* #'v/restart-not-found))

  (is (empty? v/*restarts*)))

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

(v/defrestart r)
(v/defrestart r-with-doc "doc")
(v/defrestart r-with-meta {:sbrubbles 1 :doc "sbrubbles"})
(v/defrestart r-with-obj 42)

(deftest defrestart-metadata
  (are [v field val] (= (field (meta v)) val)
       #'r :doc nil
       #'r :sbrubbles nil
       #'r :metadata nil

       #'r-with-doc :doc "doc"
       #'r-with-doc :sbrubbles nil
       #'r-with-doc :metadata nil

       #'r-with-meta :doc "sbrubbles"
       #'r-with-meta :sbrubbles 1
       #'r-with-meta :metadata nil

       #'r-with-obj :doc nil
       #'r-with-obj :sbrubbles nil
       #'r-with-obj :metadata 42))

(deftest handling
  (is (thrown? ExceptionInfo (c 1 2 3)))

  (v/handle [c list]
    (is (= (v/*handlers* #'c)
           (list list)))

    (is (= (v/signal #'c 1 2 3)
           (c 1 2 3)
           (list 1 2 3))))

  (is (thrown? ExceptionInfo (c 1 2 3))))

(deftest restarting
  (is (thrown? ExceptionInfo (r 1 2 3)))

  (v/with [r list]
    (is (= (v/*restarts* #'r)
           list))

    (is (= (v/restart #'r 1 2 3)
           (r 1 2 3)
           (list 1 2 3))))

  (let [r-prime (v/with-fn {#'r list}
                           (fn [& args] (apply r args)))]
    (is (= (r-prime 1 2 3)
           (list 1 2 3)))

    (is (thrown? ExceptionInfo (r 1 2 3))))

  (is (thrown? ExceptionInfo (r 1 2 3))))

(deftest handle-and-with
  (v/handle [c #(r %)]
    (v/with [r inc]
      (is (= (c 1) 2))))

  (v/with [r inc]
    (v/handle [c #(r %)]
      (is (= (c 1) 2)))))

(deftest skipping-handlers
  (v/handle [c inc]
    (v/handle [c #(if (even? %)
                    :even
                    (v/skip))]
      (is (= (c 1) 2))
      (is (= (c 2) :even)))))