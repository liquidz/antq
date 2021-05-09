(ns antq.core-test
  (:require
   [antq.core :as sut]
   [antq.diff.java :as d.java]
   [antq.record :as r]
   [antq.util.git :as u.git]
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
              :focus []
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

  (t/testing "--focus"
    (t/is (= ["fo/cus1" "fo/cus2" "fo/cus3"]
             (get-in (test-parse-opts ["--focus=fo/cus1"
                                       "--focus=fo/cus2:fo/cus3"])
                     [:options :focus]))))

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
        (t/is (some? (:errors res))))))

  (t/testing "--upgrade"
    (t/is (true? (get-in (test-parse-opts ["--upgrade"])
                         [:options :upgrade])))))

(t/deftest skip-artifacts?-test
  (t/testing "default"
    (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})
                                                          {}))
      false "org.clojure/clojure"
      false "org.clojure/foo"
      false "foo/clojure"
      false "foo"
      false "foo/bar"))

  (t/testing "custom: exclude"
    (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})
                                                          {:exclude ["org.clojure/clojure" "org.clojure/foo" "foo"]}))
      true "org.clojure/clojure"
      true "org.clojure/foo"
      false "foo/clojure"
      true "foo"
      false "foo/bar"))

  (t/testing "custom: focus"
    (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})
                                                          {:focus ["org.clojure/clojure" "foo"]}))
      false "org.clojure/clojure"
      true "org.clojure/foo"
      true "foo/clojure"
      false "foo"
      true "foo/bar"))

  (t/testing "`focus` shoud be prefer than `exclude`"
    (t/is (false? (sut/skip-artifacts? (r/map->Dependency {:name "org.clojure/clojure"})
                                       {:exclude ["org.clojure/clojure"]
                                        :focus ["org.clojure/clojure"]})))))

(t/deftest using-release-version?-test
  (t/are [expected in] (= expected (sut/using-release-version?
                                    (r/map->Dependency {:version in})))
    true "RELEASE"
    true "master"
    true "main"
    true "latest"
    false "1.0.0"
    false ""))

(defn- test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

(t/deftest outdated-deps-test
  (let [deps [(test-dep {:name "alice" :version "1.0.0"})
              (test-dep {:name "bob" :version "2.0.0"})
              (test-dep {:name "charlie" :version "3.0.0"})]]

    (t/is (= [(test-dep {:name "alice" :version "1.0.0" :latest-version "3.0.0"})
              (test-dep {:name "bob" :version "2.0.0" :latest-version "3.0.0"})]
             (sut/outdated-deps deps {})))

    (t/testing "alice@3.0.0 should be excluded"
      (t/is (= [(test-dep {:name "alice" :version "1.0.0" :latest-version "2.0.0"})
                (test-dep {:name "bob" :version "2.0.0" :latest-version "3.0.0"})]
               (sut/outdated-deps deps {:exclude ["alice@3.0.0"]}))))))

(t/deftest assoc-diff-url-test
  (let [dummy-dep {:type :java :name "foo/bar" :version "1" :latest-version "2"}]
    (with-redefs [d.java/get-scm-url (constantly "https://github.com/foo/bar")
                  u.git/tags-by-ls-remote (constantly ["1" "2"])]
      (t/is (= (assoc dummy-dep :diff-url "https://github.com/foo/bar/compare/1...2")
               (sut/assoc-diff-url dummy-dep)))

      (t/is (= (assoc dummy-dep :type :test)
               (sut/assoc-diff-url (assoc dummy-dep :type :test)))))))

(t/deftest unverified-deps-test
  (let [dummy-deps [{:type :java :name "antq/antq"}
                    {:type :java :name "seancorfield/next.jdbc"}
                    {:type :java :name "dummy/dummy"}
                    {:type :UNKNOWN :name "antq/antq"}]]
    (t/is (= [{:type :java
               :name "antq/antq"
               :version "antq/antq"
               :latest-version nil
               :latest-name "com.github.liquidz/antq"}
              {:type :java
               :name "seancorfield/next.jdbc"
               :version "seancorfield/next.jdbc"
               :latest-version nil
               :latest-name "com.github.seancorfield/next.jdbc"}]
             (sut/unverified-deps dummy-deps)))))

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

(t/deftest mark-only-newest-version-flag-test
  (let [deps [(r/map->Dependency {:name "org.clojure/clojure" :version "1"})
              (r/map->Dependency {:name "org.clojure/clojure" :version "2"})
              (r/map->Dependency {:name "dummy" :version "3"})]
        res (sut/mark-only-newest-version-flag deps)]
    (t/is (= 3 (count res)))
    (t/is (= #{(r/map->Dependency {:name "org.clojure/clojure" :version "1" :only-newest-version? true})
               (r/map->Dependency {:name "org.clojure/clojure" :version "2" :only-newest-version? true})
               (r/map->Dependency {:name "dummy" :version "3" :only-newest-version? nil})}
             (set res)))))

(t/deftest unify-deps-having-only-newest-version-flag-test
  (let [deps [(r/map->Dependency {:name "foo" :version "1.8.0" :file "deps.edn" :only-newest-version? true})
              (r/map->Dependency {:name "foo" :version "1.9.0" :file "deps.edn" :only-newest-version? true})
              (r/map->Dependency {:name "foo" :version "1.10.2" :file "deps.edn" :only-newest-version? true})

              (r/map->Dependency {:name "foo" :version "1.8.0" :file "project.clj" :only-newest-version? true})
              (r/map->Dependency {:name "foo" :version "1.9.0" :file "project.clj" :only-newest-version? true})

              (r/map->Dependency {:name "bar" :version "1.8.0" :file "project.clj" :only-newest-version? false})
              (r/map->Dependency {:name "bar" :version "1.9.0" :file "project.clj" :only-newest-version? false})]
        res (sut/unify-deps-having-only-newest-version-flag deps)]
    (t/is (= 4 (count res)))
    (t/is (= #{(r/map->Dependency {:name "foo" :version "1.10.2" :file "deps.edn" :only-newest-version? true})
               (r/map->Dependency {:name "foo" :version "1.9.0" :file "project.clj" :only-newest-version? true})
               (r/map->Dependency {:name "bar" :version "1.8.0" :file "project.clj" :only-newest-version? false})
               (r/map->Dependency {:name "bar" :version "1.9.0" :file "project.clj" :only-newest-version? false})}
             (set res)))))

(t/deftest latest-test
  (t/is (= "3.0.0"
           (str/trim
            (with-out-str
              (sut/latest {:type :test :name 'foo/bar}))))))
