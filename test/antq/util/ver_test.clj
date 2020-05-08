(ns antq.util.ver-test
  (:require
   [antq.util.ver :as sut]
   [clojure.test :as t]))

(t/deftest normalize-version-test
  (t/are [expected in] (= expected (sut/normalize-version in))
    "1.0.0" "v1.0.0"
    "1.0.0" "1.0.0"))

(t/deftest sem-ver?-test
  (t/are [expected in] (= expected (sut/sem-ver? in))
    true "1.0.0"
    true "1.0"
    true "1"
    false "foo"
    false "0.0-1"
    false ""))
