(ns antq.dep.clojure-test
  (:require
   [antq.dep.clojure :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private file-path
  "path/to/deps.edn")

(defn- java-dependency
  [m]
  (r/map->Dependency (merge {:project :clojure
                             :type :java
                             :file file-path
                             :repositories {"antq-test" {:url "s3://antq-repo/"}}}
                            m)))
(defn- git-dependency
  [m]
  (r/map->Dependency (merge {:project :clojure
                             :type :git-sha
                             :file file-path
                             :repositories {"antq-test" {:url "s3://antq-repo/"}}}
                            m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps
              file-path
              (slurp (io/resource "dep/deps.edn")))]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(java-dependency {:name "foo/core" :version "1.0.0"})
               (java-dependency {:name "foo/core" :version "1.1.0"})
               (java-dependency {:name "bar/bar" :version "2.0.0"})
               (java-dependency {:name "baz/baz" :version "3.0.0"})
               (java-dependency {:name "rep/rep" :version "4.0.0"})
               (java-dependency {:name "ovr/ovr" :version "5.0.0"})
               (git-dependency {:name "git/hello" :version "dummy-sha"
                                :extra {:url "https://github.com/example/hello.git"}})}
             (set deps)))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps "test/resources/dep")]
    (t/is (every? #(contains? #{:java :git-sha} (:type %)) deps))))
