(ns antq.dep.clojure.tool-test
  (:require
   [antq.dep.clojure.tool :as sut]
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private file-path
  (.getAbsolutePath (io/file (io/resource "dep/clojure-cli-tool/dummy.edn"))))

(defn- git-tag-dependency
  [m]
  (r/map->Dependency (merge {:project :clojure-tool
                             :type :git-tag-and-sha
                             :file file-path}
                            m)))

(t/deftest extract-deps-test
  (let [content (pr-str '{:lib foo/bar
                          :coord {:git/tag "1.0.0"
                                  :git/sha "dummy sha"
                                  :git/url "https://example.com"}})]
    (t/is (= (git-tag-dependency {:name "foo/bar" :version "1.0.0"
                                  :extra {:url "https://example.com" :sha "dummy sha"}})
             (sut/extract-deps file-path content)))))

(t/deftest extract-deps-local-root-test
  (let [content (pr-str '{:lib foo/bar
                          :coord {:local/root "/path/to/local"}})]
    (t/is (nil? (sut/extract-deps file-path content)))))

(t/deftest load-deps-test
  (let [dir (io/file (io/resource "dep/clojure-cli-tool"))
        deps (sut/load-deps dir)]
    (t/is (every? #(= :git-tag-and-sha (:type %)) deps))))
