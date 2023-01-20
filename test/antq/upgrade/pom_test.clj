(ns antq.upgrade.pom-test
  (:require
   [antq.dep.pom :as dep.pom]
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade :as upgrade]
   [antq.upgrade.pom]
   [clojure.java.io :as io]
   [clojure.test :as t])
  (:import
   java.io.File))

(def ^:private dummy-java-dep
  (r/map->Dependency {:project :pom
                      :type :java
                      :name "foo/core"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/test_pom.xml")}))

(def ^:private dummy-prop-java-dep
  (r/map->Dependency {:project :pom
                      :type :java
                      :name "org.clojure/clojure"
                      :latest-version "1.11.1"
                      :file (io/file (io/resource "dep/test_pom_properties.xml"))}))

(t/deftest upgrade-dep-test
  (let [tmp-file (File/createTempFile "upgrade-dep-test" "xml")]
    (try
      (let [from-deps (->> dummy-java-dep
                           :file
                           (dep.pom/extract-deps ""))

            _ (->> dummy-java-dep
                   (upgrade/upgrader)
                   (spit tmp-file))
            to-deps (dep.pom/extract-deps "" tmp-file)]
        (t/is (= #{{:name "foo/core" :version {:- "1.0.0" :+ "9.0.0"}}
                   {:name "foo/core" :version {:- "1.1.0" :+ "9.0.0"}}}
                 (h/diff-deps from-deps to-deps))))
      (finally
        (.delete tmp-file)))))

(t/deftest upgrade-dep-with-properties-test
  (let [tmp-file (File/createTempFile "upgrade-dep-test" "xml")]
    (try
      (let [from-deps (->> dummy-prop-java-dep
                           :file
                           (dep.pom/extract-deps ""))
            _ (->> dummy-prop-java-dep
                   (upgrade/upgrader)
                   (spit tmp-file))
            to-deps (dep.pom/extract-deps "" tmp-file)]
        (t/is (empty? (h/diff-deps from-deps to-deps))))
      (finally
        (.delete tmp-file)))))
