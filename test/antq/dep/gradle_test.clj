(ns antq.dep.gradle-test
  (:require
   [antq.dep.gradle :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private file-path
  "path/to/build.gradle")

(defn- java-dependency
  [m]
  (r/map->Dependency (merge {:project :gradle
                             :type :java
                             :file file-path}
                            m)))

(t/deftest extract-deps-test
  (let [deps (sut/extract-deps
              file-path
              (.getPath (io/resource "dep/build.gradle")))]
    (t/is (= #{(java-dependency {:name "nrepl/nrepl" :version "0.6.0"})
               (java-dependency {:name "org.ajoberstar/jovial" :version "0.3.0"})
               (java-dependency {:name "org.clojure/tools.namespace" :version "1.0.0"})
               (java-dependency {:name "org.clojure/clojure" :version "1.10.0"})}
             (set deps)))))

(t/deftest extract-deps-command-error-test
  (with-redefs [sut/gradle-command "__non-existing-command__"]
    (let [deps (sut/extract-deps
                file-path
                (.getPath (io/resource "dep/build.gradle")))]
      (t/is (nil? deps)))))
