(ns org.sbrubbles.conditio-test
  {:clj-kondo/config '{:lint-as {clojure.test.check.clojure-test/defspec clj-kondo.lint-as/def-catch-all}}}
  (:require
    [clojure.test :refer :all]
    [clojure.test.check.clojure-test :refer [defspec]]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [org.sbrubbles.conditio :as c]))

(defspec handle-test
  100
  (prop/for-all [keyword (gen/such-that #(not (#{::c/handler-not-found ::c/restart-not-found} %))
                                        gen/keyword)
                 value gen/any]
    (is (not (c/*handlers* keyword)))
    (c/handle [keyword value]
      (is (= (c/*handlers* keyword) value)))
    (is (not (c/*handlers* keyword)))))

(defspec with-restart-test
  100
  (prop/for-all [keyword gen/keyword
                 value gen/any]
    (is (not (c/*restarts* keyword)))
    (c/with-restarts [keyword value]
      (is (= (c/*restarts* keyword) value)))
    (is (not (c/*restarts* keyword)))))

(defspec abort-takes-anything-as-message
  100
  (prop/for-all [v gen/any]
    (try
      ((c/abort v))
      (catch clojure.lang.ExceptionInfo e
        (is (str v) (.getMessage e))))))



