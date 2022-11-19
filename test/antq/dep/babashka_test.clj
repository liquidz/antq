(ns antq.dep.babashka-test
  (:require
   [antq.dep.babashka :as sut]
   [antq.record :as r]
   [clojure.test :as t]))

(t/deftest load-deps-test
  (with-redefs [sut/project-file "test_bb.edn"]
    (t/is (= [(r/map->Dependency {:type :java
                                  :file "test/resources/dep/test_bb.edn"
                                  :name "bb/core"
                                  :version "1.0.0"
                                  :project :clojure
                                  :repositories nil})
              (r/map->Dependency {:type :java
                                  :file "test/resources/dep/test_bb.edn"
                                  :name "with/meta"
                                  :version "2.0.0"
                                  :project :clojure
                                  :repositories nil})]
             (->> (sut/load-deps "test/resources/dep")
                  (sort-by :name))))))
