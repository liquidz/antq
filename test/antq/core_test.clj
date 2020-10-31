(ns antq.core-test
  (:require
   [antq.core :as sut]
   [antq.record :as r]
   [antq.ver :as ver]
   [clojure.string :as str]
   [clojure.test :as t]
   [clojure.tools.cli :as cli]))

(defmethod ver/get-sorted-versions :test
  [_]
  ["3.0.0" "2.0.0" "1.0.0"])

(def ^:private test-parse-opts #(cli/parse-opts % sut/cli-options))

(t/deftest cli-options-test
  (t/testing "default options"
    (t/is (= {:exclude []
              :directory ["."]
              :skip []
              :error-format nil
              :reporter "table"}
             (:options (test-parse-opts [])))))

  (t/testing "--exclude"
    (t/is (= ["ex/ex1" "ex/ex2" "ex/ex3"]
             (get-in (test-parse-opts ["--exclude=ex/ex1"
                                       "--exclude=ex/ex2:ex/ex3"])
                     [:options :exclude]))))

  (t/testing "--directory"
    (t/is (= ["." "dir1" "dir2" "dir3" "dir4"]
             (get-in (test-parse-opts ["-d" "dir1"
                                       "--directory=dir2"
                                       "--directory" "dir3:dir4"])
                     [:options :directory]))))

  (t/testing "--skip"
    (let [res (test-parse-opts ["--skip" "boot"
                                "--skip=clojure-cli"])]
      (t/is (= ["boot" "clojure-cli"]
               (get-in res [:options :skip])))
      (t/is (nil? (:errors res))))

    (t/testing "validation error"
      (let [res (test-parse-opts ["--skip=foo"])]
        (t/is (= [] (get-in res [:options :skip])))
        (t/is (some? (:errors res))))))

  (t/testing "--error-format"
    (t/is (= "foo"
             (get-in (test-parse-opts ["--error-format=foo"])
                     [:options :error-format]))))

  (t/testing "--reporter"
    (let [res (test-parse-opts ["--reporter=edn"])]
      (t/is (= "edn" (get-in res [:options :reporter])))
      (t/is (nil? (:errors res))))

    (t/testing "validation error"
      (let [res (test-parse-opts ["--reporter=foo"])]
        (t/is (= "table" (get-in res [:options :reporter])))
        (t/is (some? (:errors res)))))))

(t/deftest skip-artifacts?-test
  (t/testing "default"
    (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})
                                                          {}))
      true "org.clojure/clojure"
      false "org.clojure/foo"
      false "foo/clojure"
      false "foo"
      false "foo/bar"))

  (t/testing "custom"
    (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})
                                                          {:exclude ["org.clojure/foo" "foo"]}))
      true "org.clojure/clojure"
      true "org.clojure/foo"
      false "foo/clojure"
      true "foo"
      false "foo/bar")))

(t/deftest using-release-version?-test
  (t/are [expected in] (= expected (sut/using-release-version?
                                    (r/map->Dependency {:version in})))
    true "RELEASE"
    true "master"
    false "1.0.0"
    false ""))

(defn- test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

(t/deftest outdated-deps-test
  (t/is (= [(test-dep {:name "alice" :version "1.0.0" :latest-version "3.0.0"})
            (test-dep {:name "bob" :version "2.0.0" :latest-version "3.0.0"})]
           (sut/outdated-deps [(test-dep {:name "alice" :version "1.0.0"})
                               (test-dep {:name "bob" :version "2.0.0"})
                               (test-dep {:name "charlie" :version "3.0.0"})
                               (test-dep {:name (first sut/default-skip-artifacts) :version "1.0.0"})]
                              {}))))

(t/deftest fetch-deps-test
  (t/is (seq (sut/fetch-deps {:directory ["."]})))

  (t/testing "skip"
    (t/testing "boot"
      (t/is (nil? (some #(= "test/resources/dep/build.boot" (:file %))
                        (sut/fetch-deps {:directory ["test/resources/dep"]
                                         :skip ["boot"]})))))

    (t/testing "clojure-cli"
      (t/is (nil? (some #(= "test/resources/dep/deps.edn" (:file %))
                        (sut/fetch-deps {:directory ["test/resources/dep"]
                                         :skip ["clojure-cli"]})))))

    (t/testing "github-action"
      (t/is (nil? (some #(= "test/resources/dep/github_action.yml" (:file %))
                        (sut/fetch-deps {:directory ["test/resources/dep"]
                                         :skip ["github-action"]})))))

    (t/testing "pom"
      (t/is (nil? (some #(= "test/resources/dep/pom.xml" (:file %))
                        (sut/fetch-deps {:directory ["test/resources/dep"]
                                         :skip ["pom"]})))))

    (t/testing "shadow-cljs"
      (t/is (nil? (some #(#{"test/resources/dep/shadow-cljs.edn"
                            "test/resources/dep/shadow-cljs-env.edn"} (:file %))
                        (sut/fetch-deps {:directory ["test/resources/dep"]
                                         :skip ["shadow-cljs"]})))))

    (t/testing "leiningen"
      (t/is (nil? (some #(= "test/resources/dep/project.clj" (:file %))
                        (sut/fetch-deps {:directory ["test/resources/dep"]
                                         :skip ["leiningen"]})))))))

(t/deftest latest-test
  (t/is (= "3.0.0"
           (str/trim
            (with-out-str
              (sut/latest {:type :test :name 'foo/bar}))))))
