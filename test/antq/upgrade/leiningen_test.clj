(ns antq.upgrade.leiningen-test
  (:require
   [antq.dep.leiningen :as dep.lein]
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade :as upgrade]
   [antq.upgrade.leiningen]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(t/deftest upgrade-dep-test
  (let [java-dep (r/map->Dependency {:project :leiningen
                                     :type :java
                                     :name "foo/core"
                                     :latest-version "9.0.0"
                                     :file (io/resource "dep/project.clj")})
        from-deps (->> java-dep
                       :file
                       (slurp)
                       (dep.lein/extract-deps ""))
        to-deps (->> java-dep
                     (upgrade/upgrader)
                     (dep.lein/extract-deps ""))
        [from to] (h/diff-deps from-deps to-deps)]
    (t/is (= {"foo/core" "1.0.0"} from))
    (t/is (= {"foo/core" "9.0.0"} to))))

(t/deftest upgrade-meta-dep-test
  (let [meta-dep (r/map->Dependency {:project :leiningen
                                     :type :java
                                     :name "bar/bar"
                                     :latest-version "8.0.0"
                                     :file (io/resource "dep/project.clj")})
        from-deps (->> meta-dep
                       :file
                       (slurp)
                       (dep.lein/extract-deps ""))
        to-deps (->> meta-dep
                     (upgrade/upgrader)
                     (dep.lein/extract-deps ""))
        [from to] (h/diff-deps from-deps to-deps)]
    (t/is (= {"bar/bar" "2.0.0"} from))
    (t/is (= {"bar/bar" "8.0.0"} to))))
