(ns antq.ver-test
  (:require
   [antq.ver :as sut]
   [clojure.test :as t]))

(t/deftest under-devleopment?-test
  (t/are [expected in] (= expected (sut/under-devleopment? in))
    true "foo-alpha-bar"
    true "foo-beta-bar"
    true "foo-RC-bar"
    false "foo-bar"
    false ""
    false nil))

(t/deftest  snapshot?-test
  (t/are [expected in] (= expected (sut/snapshot? in))
    true "foo-snapshot-bar"
    true "foo-SNAPSHOT-bar"
    false "foo-bar"
    false ""
    false nil))
