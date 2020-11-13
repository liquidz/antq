(ns antq.upgrade.shadow-test
  (:require
   [antq.dep.shadow :as dep.shadow]
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade :as upgrade]
   [antq.upgrade.shadow]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private dummy-java-dep
  (r/map->Dependency {:project :shadow-cljs
                      :type :java
                      :name "foo/core"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/shadow-cljs.edn")}))

(t/deftest upgrade-dep-test
  (let [from-deps (->> dummy-java-dep
                       :file
                       (slurp)
                       (dep.shadow/extract-deps ""))
        to-deps (->> dummy-java-dep
                     (upgrade/upgrader)
                     (dep.shadow/extract-deps ""))
        [from to] (h/diff-deps from-deps to-deps)]
    (t/is (= {"foo/core" "1.0.0"} from))
    (t/is (= {"foo/core" "9.0.0"} to))))
