(ns org.sbrubbles.conditio-test
  {:clj-kondo/config '{:lint-as {clojure.test.check.clojure-test/defspec clj-kondo.lint-as/def-catch-all}}}
  (:require
    [clojure.test :refer :all]
    [clojure.test.check.clojure-test :refer [defspec]]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [org.sbrubbles.conditio :as c]))

(deftest signal-test
  (testing "signal calls the handler it gets if it finds one"
    (c/handle [:test inc]
      (is (= (c/signal :test 1) 2))))

  (testing "the default behavior is to explode if given an unknown condition"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Handler not found"
                          (c/signal :nonexistent 1 2 3))))

  (testing "signal signals ::c/handler-not-found if it doesn't find the given one"
    (c/handle [::c/handler-not-found identity]
      (is (= (c/signal :nonexistent 1) :nonexistent)))))

(deftest use-restart-test
  (testing "use-restart returns the restart if it finds one"
    (c/with-restarts [:test inc]
      (is (= ((c/use-restart :test 1))
             (inc 1)))))

  (testing "the default behavior is to explode if given an unknown restart"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Restart not found"
                          ((c/use-restart :nonexistent)))))

  (testing "use-restart signals ::c/restart-not-found if it doesn't find the given restart"
    (c/handle [::c/restart-not-found (fn [& _] :test)]
      (is (= ((c/use-restart :nonexistent 1))
             :test)))))

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
    (c/with-restarts [id value]
      (is (= (c/*restarts* id) value)))
    (is (not (c/*restarts* id)))))

(defspec abort-takes-anything-as-message
  100
  (prop/for-all [v gen/any]
    (try
      ((c/abort v))
      (catch clojure.lang.ExceptionInfo e
        (is (str v) (.getMessage e))))))



