(ns antq.dep.babashka-test
  (:require
   [antq.dep.babashka :as sut]
   [antq.record :as r]
   [clojure.test :as t]))

(t/deftest load-deps-test
  (t/is (= [(r/map->Dependency {:type :java
                                :file "test/resources/dep/bb.edn"
                                :name "bb/core"
                                :version "1.0.0"
                                :project :clojure
                                :repositories nil})]
           (sut/load-deps "test/resources/dep"))))
