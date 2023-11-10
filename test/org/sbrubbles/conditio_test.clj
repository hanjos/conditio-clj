(ns org.sbrubbles.conditio-test
  (:require
    [clojure.test :refer :all]
    [org.sbrubbles.conditio :as c])
  (:import
    (clojure.lang ExceptionInfo)))

;; conditions
(deftest conditions
  (let [id :id
        con (c/condition id)]
    (is (identical? (c/condition con) con))
    (is (c/condition? con))
    (is (= (::c/id (c/condition id))
           id))

    (let [c-id (::c/id con)]
      (c/handle [c-id identity]
        (is (identical? (c/signal con) con))))))

(deftest condition-id-cannot-be-nil
  (is (thrown? AssertionError
               (c/condition nil))))

;; signal
(deftest signal-calls-the-handler
  (c/handle [:test #(inc (:input %))]
    (is (= (c/signal :test :input 1) 2))))

(deftest signal-explodes-if-given-an-unknown-condition
  (is (thrown-with-msg? ExceptionInfo #"Abort: :org.sbrubbles.conditio/handler-not-found"
                        (c/signal :nonexistent :input 1))))

(deftest signal-signals-handler-not-found
  (c/handle [::c/handler-not-found #(::c/id (:condition %))]
    (is (= (c/signal :nonexistent) :nonexistent))))

;; restart
(deftest restart-runs-the-given-restart
  (c/with [:test inc]
    (is (= (c/restart :test 1)
           (inc 1)))))

(deftest restart-explodes-if-given-an-unknown-restart
  (is (thrown-with-msg? ExceptionInfo #"Abort: :org.sbrubbles.conditio/restart-not-found"
                        (c/restart :nonexistent))))

(deftest restart-signals-restart-not-found
  (c/handle [::c/restart-not-found (fn [_] :test)]
    (is (= (c/restart :nonexistent 1)
           :test))))

;; abort
(deftest abort-explodes-when-called
  (is (thrown-with-msg? ExceptionInfo #"Abort"
                        (c/abort)))

  (is (thrown-with-msg? ExceptionInfo #"Abort on "
                        (c/abort 1)))

  (is (thrown-with-msg? ExceptionInfo #"Abort on "
                        (c/abort :args)))

  (is (thrown-with-msg? ExceptionInfo #"Abort on "
                        (c/abort {}))))

;; handle, with and with-fn
(deftest handle-and-with
  (let [id :id
        value "value"]
    (is (not (c/*handlers* id)))
    (c/handle [id value]
      (is (= (c/*handlers* id) (list value c/abort))))
    (is (not (c/*handlers* id)))

    (is (not (c/*restarts* id)))
    (c/with [id value]
      (is (= (c/*restarts* id) value)))
    (is (not (c/*restarts* id)))

    (let [f #(c/restart id)
          with-restarts-f (c/with-fn {id (fn [] value)} f)]
      (is (thrown-with-msg? ExceptionInfo #"Abort: :org.sbrubbles.conditio/restart-not-found" (f)))
      (c/with [id (fn [] value)]
        (is (= (f) value)))
      (is (= (with-restarts-f) value)))))

(deftest with-and-handle-together
  (c/handle [:condition #(c/restart :restart (:n %))]
    (c/with [:restart inc]
      (is (= (c/signal :condition :n 1)
             (inc 1))))))

;; skipping
(deftest skipping
  (c/handle [:else (fn [_] :success)]
    (c/handle [:else c/skip]
      (is (= (c/signal :else)
             :success))))

  (c/handle [:else (fn [_] 2)]
    (c/handle [:else (fn [_] 1)]
      (c/handle [:else c/skip]
        (is (= (c/signal :else)
               1))))))

(deftest skip-0-and-1-return-the-same
  (is (= (c/skip)
         (c/skip :doesnt-matter))))