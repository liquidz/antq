(ns antq.dep.github-action-test
  (:require
   [antq.dep.github-action :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(defn- git-tag-dependency
  [m]
  (r/map->Dependency (merge {:project :github-action
                             :type :github-tag
                             :file "dep/github_action.yml"} m)))

(defn- git-sha-dependency
  [m]
  (r/map->Dependency (merge {:project :github-action
                             :type :git-sha
                             :file "dep/github_action.yml"} m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps
              "dep/github_action.yml"
              (slurp (io/resource "dep/github_action.yml")))]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(git-tag-dependency {:name "foo/bar" :version "v1.0.0"})
               (git-tag-dependency {:name "bar/baz" :version "master"})
               (git-sha-dependency {:name "git/sha" :version "8be09192b01d78912b03852f5d6141e8c48f4179"
                                    :extra {:url "https://github.com/git/sha.git"}})
               (git-sha-dependency {:name "git/sha-short" :version "8be0919"
                                    :extra {:url "https://github.com/git/sha-short.git"}})}
             (set deps)))))

(t/deftest extract-deps-matrix-test
  (let [deps (sut/extract-deps
              "dep/github_action_matrix.yml"
              (slurp (io/resource "dep/github_action_matrix.yml")))
        git-tag-dependency #(git-tag-dependency (merge {:file "dep/github_action_matrix.yml"} %))]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(git-tag-dependency {:name "DeLaGuardo/setup-graalvm" :version "master" :only-newest-version? nil})
               (git-tag-dependency {:name "graalvm/graalvm-ce-builds" :version "v2.0.0" :only-newest-version? true})
               (git-tag-dependency {:name "graalvm/graalvm-ce-builds" :version "v3.0.0" :only-newest-version? true})}
             (set deps)))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps)]
    (t/is (= #{".github/workflows/coverage.yml"
               ".github/workflows/dependencies.yml"
               ".github/workflows/release.yml"
               ".github/workflows/lint.yml"
               ".github/workflows/reviewdog.yml"
               ".github/workflows/test.yml"}
             (set (map :file deps))))))
