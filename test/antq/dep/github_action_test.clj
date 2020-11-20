(ns antq.dep.github-action-test
  (:require
   [antq.dep.github-action :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(defn- git-tag-dependency
  [m]
  (r/map->Dependency (merge {:project :github-action
                             :type :github-action
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
    (t/is (= #{(git-tag-dependency {:name "foo/bar" :version "1.0.0"})
               (git-tag-dependency {:name "bar/baz" :version "master"})
               (git-sha-dependency {:name "git/sha" :version "8be09192b01d78912b03852f5d6141e8c48f4179"
                                    :extra {:url "https://github.com/git/sha.git"}})}
             (set deps)))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps)]
    (t/is (every? #(contains? #{:github-action :git-sha} (:type %)) deps))
    (t/is (= #{".github/workflows/coverage.yml"
               ".github/workflows/dependencies.yml"
               ".github/workflows/docker.yml"
               ".github/workflows/lint.yml"
               ".github/workflows/reviewdog.yml"
               ".github/workflows/static.yml"
               ".github/workflows/test.yml"}
             (set (map :file deps))))))
