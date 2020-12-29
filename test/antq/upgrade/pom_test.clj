(ns antq.upgrade.pom-test
  (:require
   [antq.dep.pom :as dep.pom]
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade :as upgrade]
   [antq.upgrade.pom]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private dummy-java-dep
  (r/map->Dependency {:project :pom
                      :type :java
                      :name "foo/core"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/pom.xml")}))

(t/deftest upgrade-dep-test
  (let [from-deps (->> dummy-java-dep
                       :file
                       (slurp)
                       (dep.pom/extract-deps ""))
        to-deps (->> dummy-java-dep
                     (upgrade/upgrader)
                     (dep.pom/extract-deps ""))]
    (t/is (= #{{:name "foo/core" :version {:- "1.0.0" :+ "9.0.0"}}}
             (h/diff-deps from-deps to-deps)))))
