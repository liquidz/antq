(ns antq.ver-test
  (:require
   [antq.record :as r]
   [antq.ver :as sut]
   [clojure.test :as t]))

(t/deftest under-development?-test
  (t/are [expected in] (= expected (sut/under-development? in))
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

(t/deftest latest?-test
  (t/are [expected version latest-version]
         (= expected (sut/latest?
                      (r/map->Dependency {:version version :latest-version latest-version})))
    true "1.0.0" "1.0.0"
    false "1.0.0" "2.0.0"
    true "2.0.0" "1.0.0"
    nil "1.0.0" nil
    nil nil "2.0.0"
    nil nil nil))
