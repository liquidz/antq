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

(t/deftest cli-options-test
  (let [args [;; --exclude
              "--exclude=ex/ex1"
              "--exclude=ex/ex2:ex/ex3"
              ;; --directory
              "-d" "dir1"
              "--directory=dir2"
              "--directory" "dir3:dir4"]
        ret (cli/parse-opts args sut/cli-options)]
    (t/is (= {:options {:exclude ["ex/ex1" "ex/ex2" "ex/ex3"]
                        :directory ["." "dir1" "dir2" "dir3" "dir4"]
                        :error-format nil
                        :reporter "table"}}
             (select-keys ret [:options])))))

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
  (t/is (seq (sut/fetch-deps {:directory ["."]}))))

(t/deftest latest-test
  (t/is (= "3.0.0"
           (str/trim
            (with-out-str
              (sut/latest {:type :test :name 'foo/bar}))))))
