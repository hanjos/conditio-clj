(ns org.sbrubbles.conditio-test
  {:clj-kondo/config '{:lint-as {clojure.test.check.clojure-test/defspec clj-kondo.lint-as/def-catch-all}}}
  (:require
    [clojure.test :refer :all]
    [clojure.test.check.clojure-test :refer [defspec]]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [org.sbrubbles.conditio :as c])
  (:import
    (clojure.lang ExceptionInfo)))

(deftest signal-test
  (testing "signal calls the handler it gets if it finds one"
    (c/handle [:test #(inc (:input %))]
      (is (= (c/signal :test :input 1) 2))))

  (testing "the default behavior is to explode if given an unknown condition"
    (is (thrown-with-msg? ExceptionInfo #"Handler not found"
                          (c/signal :nonexistent :input 1))))

  (testing "signal signals ::c/handler-not-found if it doesn't find the given one"
    (c/handle [::c/handler-not-found #(::c/id (:condition %))]
      (is (= (c/signal :nonexistent) :nonexistent)))))

(deftest use-restart-test
  (testing "use-restart returns the restart if it finds one"
    (c/with [:test inc]
            (is (= ((c/restart :test 1))
                   (inc 1)))))

  (testing "the default behavior is to explode if given an unknown restart"
    (is (thrown-with-msg? ExceptionInfo #"Restart not found"
                          ((c/restart :nonexistent)))))

  (testing "use-restart signals ::c/restart-not-found if it doesn't find the given restart"
    (c/handle [::c/restart-not-found (fn [& _] :test)]
      (is (= ((c/restart :nonexistent 1))
             :test)))))

(deftest abort-test
  (testing "abort returns a function which explodes when called"
    (is (thrown? ExceptionInfo
                 ((c/abort))))))

(defspec handle-registers-handlers-only-in-its-context
  100
  (prop/for-all [id (gen/such-that #(not (#{::c/handler-not-found ::c/restart-not-found} %))
                                   gen/any-equatable)
                 value gen/any]
    (is (not (c/*handlers* id)))

    (c/handle [id value]
      (is (= (c/*handlers* id) value)))

    (is (not (c/*handlers* id)))))

(defspec with-restart-registers-restarts-only-in-its-context
  100
  (prop/for-all [id gen/any-equatable
                 value gen/any]
    (is (not (c/*restarts* id)))

    (c/with [id value]
            (is (= (c/*restarts* id) value)))

    (is (not (c/*restarts* id)))))

(defspec with-restart-fn-works-just-like-with-restarts
  100
  (prop/for-all [id gen/any-equatable
                 value gen/any-equatable]
    (let [f (c/restart id)
          with-restarts-f (c/with-fn f {id (fn [] value)})]

      (is (thrown-with-msg? ExceptionInfo #"Restart not found" (f)))

      (c/with [id (fn [] value)]
              (is (= (f) value)))

      (is (= (with-restarts-f) value)))))



