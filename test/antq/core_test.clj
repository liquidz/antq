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
