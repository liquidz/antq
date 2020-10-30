(ns antq.dep.shadow-test
  (:require
   [antq.dep.shadow :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private file-path
  "path/to/shadow-cljs.edn")

(defn- dependency
  [m]
  (r/map->Dependency (merge {:type :java
                             :file file-path}
                            m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps
              file-path
              (slurp (io/resource "dep/shadow-cljs.edn")))]
    (t/is (sequential? deps))
    (t/is (every? #(instance? antq.record.Dependency %) deps))
    (t/is (= #{(dependency {:name "foo/core" :version "1.0.0"})
               (dependency {:name "bar" :version "2.0.0"})
               (dependency {:name "baz" :version "3.0.0"})}
             (set deps)))))

(t/deftest extract-deps-with-env-tag-test
  (with-redefs [sut/getenv  {"ENV1" "1.0.0"
                             "ENV2" "2.0.0"
                             "ENV5" "5.0.0"}]

    (let [deps (sut/extract-deps
                file-path
                (slurp (io/resource "dep/shadow-cljs-env.edn")))]
      (t/is (sequential? deps))
      (t/is (every? #(instance? antq.record.Dependency %) deps))
      (t/is (= #{(dependency {:name "foo1" :version "1.0.0"})
                 (dependency {:name "foo2" :version "2.0.0"})
                 (dependency {:name "foo3" :version "default3"})
                 (dependency {:name "foo4" :version "default4"})
                 (dependency {:name "foo5" :version "5.0.0"})}
               (set deps))))))
