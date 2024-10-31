(ns antq.dep.circle-ci-test
  (:require
   [antq.dep.circle-ci :as sut]
   [antq.record :as r]
   [clojure.test :as t]
   [clojure.java.io :as io]))

(defn- git-tag-dependency
  [m]
  (r/map->Dependency (merge {:project :circle-ci
                             :type :circle-ci-orb
                             :file "dep/test_circle_ci.yml"} m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps "dep/test_circle_ci.yml"
                               (slurp (io/resource "dep/test_circle_ci.yml")))]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(git-tag-dependency {:name "circleci/node" :version "6.3.0"})
               (git-tag-dependency {:name "circleci/docker" :version "2.8.0"})}
              (set deps)))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps)]
    (t/is (= #{".circleci/config.yml"}
             (set (map :file deps)))))

  (t/is (nil? (sut/load-deps "non_existing_directory"))))
