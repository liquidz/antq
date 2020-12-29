(ns antq.upgrade.clojure-test
  (:require
   [antq.dep.clojure :as dep.clj]
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade :as upgrade]
   [antq.upgrade.clojure]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private dummy-java-dep
  (r/map->Dependency {:project :clojure
                      :type :java
                      :name "foo/core"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/deps.edn")}))

(def ^:private dummy-git-dep
  (r/map->Dependency {:project :clojure
                      :type :git-sha
                      :name "git/hello"
                      :latest-version "new-sha"
                      :file (io/resource "dep/deps.edn")}))

(t/deftest upgrade-dep-test
  (t/testing "java"
    (let [from-deps (->> dummy-java-dep
                         :file
                         (slurp)
                         (dep.clj/extract-deps ""))
          to-deps (->> dummy-java-dep
                       (upgrade/upgrader)
                       (dep.clj/extract-deps ""))
          [from to] (h/diff-deps from-deps to-deps)]
      (t/is (= {"foo/core" "1.0.0"} from))
      (t/is (= {"foo/core" "9.0.0"} to))))

  (t/testing "git"
    (let [from-deps (->> dummy-git-dep
                         :file
                         (slurp)
                         (dep.clj/extract-deps ""))
          to-deps (->> dummy-git-dep
                       (upgrade/upgrader)
                       (dep.clj/extract-deps ""))
          [from to] (h/diff-deps from-deps to-deps)]
      (t/is (= {"git/hello" "dummy-sha"} from))
      (t/is (= {"git/hello" "new-sha"} to)))))

(t/deftest upgrade-dep-replce-deps-test
  (let [dummy-dep (assoc dummy-java-dep :name "rep")
        from-deps (->> dummy-dep
                       :file
                       (slurp)
                       (dep.clj/extract-deps ""))
        to-deps (->> dummy-dep
                     (upgrade/upgrader)
                     (dep.clj/extract-deps ""))
        [from to] (h/diff-deps from-deps to-deps)]
    (t/is (= {"rep/rep" "4.0.0"} from))
    (t/is (= {"rep/rep" "9.0.0"} to))))
