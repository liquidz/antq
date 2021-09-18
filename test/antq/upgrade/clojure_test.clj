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
                      :name "sha/sha"
                      :latest-version "new-sha"
                      :file (io/resource "dep/deps.edn")}))

(def ^:private dummy-git-git-sha-dep
  (r/map->Dependency {:project :clojure
                      :type :git-sha
                      :name "git-sha/git-sha"
                      :latest-version "new-sha"
                      :file (io/resource "dep/deps.edn")}))

(def ^:private dummy-no-version-dep
  (r/map->Dependency {:project :clojure
                      :type :java
                      :name "no-version"
                      :latest-version "9.9.9"
                      :file (io/resource "dep/deps.edn")}))

(t/deftest upgrade-dep-test
  (t/testing "java"
    (let [from-deps (->> dummy-java-dep
                         :file
                         (slurp)
                         (dep.clj/extract-deps ""))
          to-deps (->> dummy-java-dep
                       (upgrade/upgrader)
                       (dep.clj/extract-deps ""))]
      (t/is (= #{{:name "foo/core" :version {:- "1.0.0" :+ "9.0.0"}}
                 {:name "foo/core" :version {:- "1.1.0" :+ "9.0.0"}}}
               (h/diff-deps from-deps to-deps)))))

  (t/testing "git :sha"
    (let [from-deps (->> dummy-git-dep
                         :file
                         (slurp)
                         (dep.clj/extract-deps ""))
          to-deps (->> dummy-git-dep
                       (upgrade/upgrader)
                       (dep.clj/extract-deps ""))]
      (t/is (= #{{:name "sha/sha"
                  :version {:- "dummy-sha" :+ "new-sha"}
                  :url "https://github.com/example/sha.git"}}
               (h/diff-deps from-deps to-deps)))))

  (t/testing "git :git/sha"
    (let [from-deps (->> dummy-git-git-sha-dep
                         :file
                         (slurp)
                         (dep.clj/extract-deps ""))
          to-deps (->> dummy-git-git-sha-dep
                       (upgrade/upgrader)
                       (dep.clj/extract-deps ""))]
      (t/is (= #{{:name "git-sha/git-sha"
                  :version {:- "dummy-git-sha" :+ "new-sha"}
                  :url "https://github.com/example/git-sha.git"}}
               (h/diff-deps from-deps to-deps)))))

  (t/testing "no corresponding value"
    (let [from-deps (->> dummy-no-version-dep
                         :file
                         (slurp)
                         (dep.clj/extract-deps ""))
          to-deps (->> dummy-no-version-dep
                       (upgrade/upgrader)
                       (dep.clj/extract-deps ""))]
      (t/is (empty? (h/diff-deps from-deps to-deps))))))

(t/deftest upgrade-dep-replce-deps-test
  (let [dummy-dep (assoc dummy-java-dep :name "rep")
        from-deps (->> dummy-dep
                       :file
                       (slurp)
                       (dep.clj/extract-deps ""))
        to-deps (->> dummy-dep
                     (upgrade/upgrader)
                     (dep.clj/extract-deps ""))]
    (t/is (= #{{:name "rep/rep" :version {:- "4.0.0" :+ "9.0.0"}}}
             (h/diff-deps from-deps to-deps)))))

(t/deftest upgrade-dep-override-deps-test
  (let [dummy-dep (assoc dummy-java-dep :name "ovr")
        from-deps (->> dummy-dep
                       :file
                       (slurp)
                       (dep.clj/extract-deps ""))
        to-deps (->> dummy-dep
                     (upgrade/upgrader)
                     (dep.clj/extract-deps ""))]
    (t/is (= #{{:name "ovr/ovr" :version {:- "5.0.0" :+ "9.0.0"}}}
             (h/diff-deps from-deps to-deps)))))
