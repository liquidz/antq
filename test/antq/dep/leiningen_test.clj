(ns antq.dep.leiningen-test
  (:require
   [antq.dep.leiningen :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private file-path
  "path/to/project.clj")

(defn- dependency
  [m]
  (r/map->Dependency (merge {:project :leiningen
                             :type :java
                             :file file-path
                             :repositories {"antq-test" {:url "s3://antq-repo/"}
                                            "str-test" {:url "https://example.com"}}}
                            m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps
              file-path
              (slurp (io/resource "dep/project.clj")))]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(dependency {:name "foo/core" :version "1.0.0"})
               (dependency {:name "foo/core" :version "1.1.0"})
               (dependency {:name "bar/bar" :version "2.0.0"})
               (dependency {:name "baz/baz" :version "3.0.0"})}
             (set deps)))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps "test/resources/dep")]
    (t/is (every? #(= :java (:type %)) deps))))
