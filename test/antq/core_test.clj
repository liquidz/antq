(ns antq.core-test
  (:require
   [antq.core :as sut]
   [antq.record :as r]
   [antq.ver :as ver]
   [clojure.test :as t]))

(defmethod ver/get-sorted-versions :test
  [_]
  ["3.0.0" "2.0.0" "1.0.0"])

(t/deftest skip-artifacts?-test
  (t/are [expected in] (= expected (sut/skip-artifacts? (r/map->Dependency {:name in})))
    true "org.clojure/clojure"
    false "org.clojure/foo"
    false "foo/clojure"
    false "foo"
    false "foo/bar"))

(t/deftest using-release-version?-test
  (t/are [expected in] (= expected (sut/using-release-version?
                                    (r/map->Dependency {:version in})))
    true "RELEASE"
    true "master"
    false "1.0.0"
    false ""))

(t/deftest latest?-test
  (t/are [expected version latest-version]
         (= expected (sut/latest?
                      (r/map->Dependency {:version version :latest-version latest-version})))
    true "1.0.0" "1.0.0"
    false "1.0.0" "2.0.0"
    false "2.0.0" "1.0.0"
    nil "1.0.0" nil
    nil nil "2.0.0"
    nil nil nil))

(defn- test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

(t/deftest outdated-deps-test
  (t/is (= [(test-dep {:name "alice" :version "1.0.0" :latest-version "3.0.0"})
            (test-dep {:name "bob" :version "2.0.0" :latest-version "3.0.0"})]
           (sut/outdated-deps [(test-dep {:name "alice" :version "1.0.0"})
                               (test-dep {:name "bob" :version "2.0.0"})
                               (test-dep {:name "charlie" :version "3.0.0"})
                               (test-dep {:name (first sut/default-skip-artifacts) :version "1.0.0"})]))))

(t/deftest skip-duplicated-file-name-test
  (t/is (= [{:file "foo"} {:file "bar"} {:file "baz"}]
           (sut/skip-duplicated-file-name
            [{:file "foo"} {:file "bar"} {:file "baz"}])))
  (t/is (= [{:file "foo"} {:file ""} {:file "bar"}]
           (sut/skip-duplicated-file-name
            [{:file "foo"} {:file "foo"} {:file "bar"}])))
  (t/is (= [{:file "bar"} {:file "baz"} {:file ""}]
           (sut/skip-duplicated-file-name
            [{:file "bar"} {:file "baz"} {:file "baz"}]))))

(t/deftest compare-deps-test
  (let [aaa (test-dep {:file "aa" :name "bb"})
        bbb (test-dep {:file "aa" :name "xx"})
        ccc (test-dep {:file "xx" :name "bb"})
        ddd (test-dep {:file "xx" :name "xx"})]
    (t/are [expected-fn x y] (expected-fn (sut/compare-deps x y))
      zero? aaa aaa
      neg?  aaa bbb
      neg?  aaa ccc
      neg?  aaa ddd

      pos?  bbb aaa
      zero? bbb bbb
      neg?  bbb ccc
      neg?  bbb ddd

      pos?  ccc aaa
      pos?  ccc bbb
      zero? ccc ccc
      neg?  ccc ddd

      pos?  ddd aaa
      pos?  ddd bbb
      pos?  ddd ccc
      zero? ddd ddd)))

(t/deftest print-deps-test
  (let [dummy-deps [(test-dep {:file "a" :name "foo" :version "1" :latest-version "2"})
                    (test-dep {:file "b" :name "bar" :version "1" :latest-version nil})]]
    (t/is (seq (with-out-str (sut/print-deps dummy-deps))))))

(t/deftest fetch-deps-test
  (t/is (seq (sut/fetch-deps))))
