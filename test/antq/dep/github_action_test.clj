(ns antq.dep.github-action-test
  (:require
   [antq.dep.github-action :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(defn- dependency
  [m]
  (r/map->Dependency (merge {:type :github-action
                             :file "dep/github_action.yml"} m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps
              (slurp (io/resource "dep/github_action.yml"))
              "dep/github_action.yml")]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(dependency {:name "foo/bar" :version "1.0.0"})
               (dependency {:name "bar/baz" :version "master"})}
             (set deps)))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps)]
    (t/is (every? #(= :github-action (:type %)) deps))
    (t/is (= #{"./.github/workflows/dependencies.yml"
               "./.github/workflows/lint.yml"
               "./.github/workflows/static.yml"
               "./.github/workflows/test.yml"}
             (set (map :file deps))))))
