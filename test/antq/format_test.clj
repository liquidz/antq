(ns antq.format-test
  (:require
   [antq.format :as sut]
   [antq.record :as r]
   [clojure.string :as str]
   [clojure.test :as t]))

(defn- test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

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
    (t/is (seq (with-out-str (sut/print-deps
                              dummy-deps {}))))

    (t/is (seq (with-out-str (sut/print-deps
                              dummy-deps
                              {:error-format "::error file={{file}}::{{message}}"}))))

    (t/is (= ["::error file=a::foo,1,2" "::error file=b::bar,1,"]
             (str/split-lines
              (with-out-str
                (sut/print-deps
                 dummy-deps
                 {:error-format "::error file={{file}}::{{name}},{{version}},{{latest-version}}"})))))))
