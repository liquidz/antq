(ns antq.diff.java-test
  (:require
   [antq.diff :as diff]
   [antq.diff.java :as sut]
   [antq.record :as r]
   [antq.util.git :as u.git]
   [antq.util.maven :as u.mvn]
   [clojure.test :as t])
  (:import
   (org.apache.maven.model
    Model
    Scm)))

(defn- gen-dummy-model
  [^String scm-url]
  (let [scm (doto (Scm.)
              (.setUrl scm-url))]
    (doto (Model.)
      (.setScm scm))))

(t/deftest get-diff-url-test
  (let [dep (r/map->Dependency {:type :java
                                :name "foo/bar"
                                :version "1.0"
                                :latest-version "2.0"})]
    (t/testing "https://github.com"
      (with-redefs [sut/get-repository-url (constantly "https://example.com")
                    u.mvn/read-pom (fn [url]
                                     (when (= "https://example.com/foo/bar/1.0/bar-1.0.pom" url)
                                       (gen-dummy-model "https://github.com/bar/baz")))
                    u.git/tags-by-ls-remote (fn [url]
                                              (when (= "https://github.com/bar/baz/" url)
                                                ["v0.0" "v1.0" "v2.0" "v3.0"]))]
        (t/is (= "https://github.com/bar/baz/compare/v1.0...v2.0"
                 (diff/get-diff-url dep)))))

    (t/testing "git@github.com"
      (with-redefs [sut/get-repository-url (constantly "https://example.com")
                    u.mvn/read-pom (fn [url]
                                     (when (= "https://example.com/git/at/1.0/at-1.0.pom" url)
                                       (gen-dummy-model "git@github.com:git/at")))
                    u.git/tags-by-ls-remote (fn [url]
                                              (when (= "https://github.com/git/at/" url)
                                                ["v0.0" "v1.0" "v2.0" "v3.0"]))]
        (t/is (= "https://github.com/git/at/compare/v1.0...v2.0"
                 (diff/get-diff-url (assoc dep :name "git/at"))))))

    (t/testing "Failed to fetch repository URL"
      (with-redefs [sut/get-repository-url (constantly nil)]
        (t/is (nil? (diff/get-diff-url (assoc dep :name "fetch/repo-url"))))))

    (t/testing "POM not found"
      (with-redefs [sut/get-repository-url (constantly "https://example2.com")
                    u.mvn/read-pom (fn [_] (throw (java.io.FileNotFoundException. "test exception")))]
        (t/is (nil? (diff/get-diff-url (assoc dep :name "pom/not-found"))))))

    (t/testing "POM does not have SCM"
      (with-redefs [sut/get-repository-url (constantly "https://example.com")
                    u.mvn/read-pom (fn [url]
                                     (when (= "https://example.com/pom/noscm/1.0/noscm-1.0.pom" url)
                                       (doto (Model.)
                                         (.setUrl "https://github.com/pom/no-scm"))))
                    u.git/tags-by-ls-remote (fn [url]
                                              (when (= "https://github.com/pom/no-scm/" url)
                                                ["v0.0" "v1.0" "v2.0" "v3.0"]))]
        (t/is (= "https://github.com/pom/no-scm/compare/v1.0...v2.0"
                 (diff/get-diff-url (assoc dep :name "pom/noscm"))))))

    (t/testing "not supported URL"
      (with-redefs [sut/get-repository-url (constantly "https://example.com")
                    u.mvn/read-pom (fn [url]
                                     (when (= "https://example.com/not/supported/1.0/supported-1.0.pom" url)
                                       (gen-dummy-model "https://not-supported.com")))]
        (t/is (nil? (diff/get-diff-url (assoc dep :name "not/supported"))))))))
