(ns antq.dep.clojure-test
  (:require
   [antq.dep.clojure :as sut]
   [antq.record :as r]
   [antq.util.env :as u.env]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private file-path
  ;; "path/to/deps.edn"
  (.getAbsolutePath (io/file (io/resource "dep/deps.edn"))))

(defn- java-dependency
  [m]
  (r/map->Dependency (merge {:project :clojure
                             :type :java
                             :file file-path
                             :repositories {"antq-test" {:url "s3://antq-repo/"}}}
                            m)))
(defn- git-sha-dependency
  [m]
  (r/map->Dependency (merge {:project :clojure
                             :type :git-sha
                             :file file-path
                             :repositories {"antq-test" {:url "s3://antq-repo/"}}}
                            m)))

(defn- git-tag-dependency
  [m]
  (r/map->Dependency (merge {:project :clojure
                             :type :git-tag-and-sha
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
               (git-sha-dependency {:name "sha/sha" :version "dummy-sha"
                                    :extra {:url "https://github.com/example/sha.git"}})
               (git-sha-dependency {:name "git-sha/git-sha" :version "dummy-git-sha"
                                    :extra {:url "https://github.com/example/git-sha.git"}})
               (git-tag-dependency {:name "tag-short-sha/tag-short-sha" :version "v1.2.3"
                                    :extra {:url "https://github.com/example/tag-short.git"
                                            :sha "123abcd"}})
               (git-tag-dependency {:name "git-tag-long-sha/git-tag-long-sha" :version "v2.3.4"
                                    :extra {:url "https://github.com/example/git-tag-long.git"
                                            :sha "1234567890abcdefghijklmnopqrstuvwxyz1234"}})
               (git-sha-dependency {:name "com.github.liquidz/dummy"
                                    :version "dummy-inferring-url"
                                    :extra {:url "https://github.com/liquidz/dummy.git"}})
               (java-dependency {:name "local/core" :version "9.9.9"
                                 :file (.getAbsolutePath (io/file (io/resource "dep/local/deps.edn")))
                                 :repositories {}})
               (java-dependency {:name "local/nested-core" :version "8.8.8"
                                 :file (.getAbsolutePath (io/file (io/resource "dep/local/nested/deps.edn")))
                                 :repositories {}})}
             (set deps)))))

(t/deftest extract-deps-cross-project-configuration-test
  (let [cross-project-dir (.getAbsolutePath
                           (io/file
                            (.getParentFile (io/file (io/resource "dep/deps.edn")))
                            "cross-project"))
        content (pr-str '{:deps {foo/bar {:mvn/version "0.0.1"}}})]
    (t/testing "CLJ_CONFIG"
      (with-redefs [u.env/getenv #(when (= "CLJ_CONFIG" %) cross-project-dir)]
        (t/is (= [(java-dependency
                   {:name "foo/bar"
                    :version "0.0.1"
                    :file "dummy"
                    :repositories {"clj-config" {:url "https://clj-config.example.com"}}})]
                 (sut/extract-deps "dummy" content)))))

    (t/testing "XDG_CONFIG_HOME"
      (with-redefs [u.env/getenv #(when (= "XDG_CONFIG_HOME" %) cross-project-dir)]
        (t/is (= [(java-dependency
                   {:name "foo/bar"
                    :version "0.0.1"
                    :file "dummy"
                    :repositories {"xdg-config-home" {:url "https://xdg-config-home.example.com"}}})]
                 (sut/extract-deps "dummy" content)))))

    (t/testing "HOME"
      (with-redefs [u.env/getenv #(when (= "HOME" %) cross-project-dir)]
        (t/is (= [(java-dependency
                   {:name "foo/bar"
                    :version "0.0.1"
                    :file "dummy"
                    :repositories {"home" {:url "https://home.example.com"}}})]
                 (sut/extract-deps "dummy" content)))))))

(t/deftest extract-deps-unexpected-test
  (t/is (empty? (sut/extract-deps file-path "[:deps \"foo\"]")))
  (t/is (empty? (sut/extract-deps file-path "{:deps \"foo\"}")))
  (t/is (empty? (sut/extract-deps file-path "{:deps {foo/core \"bar\"}}"))))

(t/deftest load-deps-test
  (let [deps (sut/load-deps "test/resources/dep")]
    (t/is (every? #(contains? #{:java :git-sha :git-tag-and-sha} (:type %)) deps))))
