(ns antq.upgrade.leiningen-test
  (:require
   [antq.dep.leiningen :as dep.lein]
   [antq.record :as r]
   [antq.upgrade :as upgrade]
   [antq.upgrade.leiningen]
   [clojure.data :as data]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(defn- name-version-map
  [deps]
  (->> deps
       (map (juxt :name :version))
       (into {})))

(defn- diff-deps
  [from-str to-str]
  (data/diff (name-version-map (dep.lein/extract-deps "" from-str))
             (name-version-map (dep.lein/extract-deps "" to-str))))

(def dummy-version-checked-dep
  (r/map->Dependency {:project :leiningen
                      :name "foo/core"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/project.clj")}))

(t/deftest upgrade-dep-test
  (let [[from to] (diff-deps (-> dummy-version-checked-dep :file slurp)
                             (upgrade/upgrader dummy-version-checked-dep))]
    (t/is (= {"foo/core" "1.0.0"} from))
    (t/is (= {"foo/core" "9.0.0"} to))))
