(ns antq.core-test
  (:require
   [antq.core :as sut]
   [antq.record :as r]
   [clojure.test :as t]))

(t/deftest skip-artifacts?-test
  (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})))
    true "org.clojure/clojure"
    false "org.clojure/foo"
    false "foo/clojure"
    false "foo"
    false "foo/bar"))

(t/deftest skip-duplicated-file-name-test
  (t/is (= [{:file "foo"} {:file "bar"} {:file "baz"}]
           (sut/skip-duplicated-file-name
            [{:file "foo"} {:file "bar"} {:file "baz"}])))
  (t/is (= [{:file "foo"} {:file ""} {:file "bar"}]
           (sut/skip-duplicated-file-name
            [{:file "foo"} {:file "foo"} {:file "bar"}])))
  (t/is (= [{:file "bar"} {:file "baz"} {:file ""}]
           (sut/skip-duplicated-file-name
            [{:file "bar"} {:file "baz"} {:file "baz"}]))))
