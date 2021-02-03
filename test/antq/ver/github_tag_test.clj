(ns antq.ver.github-tag-test
  (:require
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.github-tag :as sut]
   [cheshire.core :as json]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.test :as t]))

(defn- dep
  [m]
  (r/map->Dependency (merge {:type :github-tag} m)))

(t/deftest tag-api-url-test
  (t/are [expected in] (= expected (sut/tag-api-url {:name in}))
    "https://api.github.com/repos/foo/bar/tags" "foo/bar"
    "https://api.github.com/repos/foo/bar/tags" "foo/bar/baz"))

(defn- get-sorted-versions
  [m]
  (ver/get-sorted-versions (dep (merge {:version "1.0.0"} m))))

(def ^:private dummy-json
  (json/generate-string
   [{:name "v1.0.0"}
    {:name "v2.0.0"}
    {:name "v3.0.0"}]))

(defn- reset-fixture
  [f]
  (reset! (deref #'sut/failed-to-fetch-from-api) false)
  (f))

(t/use-fixtures :each reset-fixture)

(t/deftest get-sorted-versions-test
  (with-redefs [slurp (constantly dummy-json)]
    (t/is (= ["v3.0.0" "v2.0.0" "v1.0.0"]
             (get-sorted-versions {:name "foo/bar"}))))

  (t/testing "response should be cached"
    (t/is (= ["v3.0.0" "v2.0.0" "v1.0.0"]
             (get-sorted-versions {:name "foo/bar"})))))

(t/deftest get-sorted-versions-fallback-test
  (let [api-errored (atom false)
        dummy-out (->> [["foo-sha" "FOO"]
                        ["one-sha" "refs/tags/1.0"]
                        ["two-sha" "refs/tags/2.0"]
                        ["bar-sha" "BAR"]]
                       (map #(str/join "\t" %))
                       (str/join "\n"))]
    (with-redefs [slurp (fn [& _]
                          (reset! api-errored true)
                          (throw (Exception. "test exception")))
                  sh/sh (fn [& args]
                          (when (= ["git" "ls-remote" "https://github.com/bar/baz"] args)
                            {:out dummy-out}))]
      (t/testing "pre"
        (t/is (false? @api-errored))
        (t/is (false? @(deref #'sut/failed-to-fetch-from-api))))

      (t/is (= ["2.0" "1.0"]
               (get-sorted-versions {:name "bar/baz"})))

      (t/testing "post"
        (t/is (true? @api-errored))
        (t/is (true? @(deref #'sut/failed-to-fetch-from-api)))))))

(defn- latest?
  [m]
  (ver/latest? (dep m)))

(t/deftest latest?-test
  (t/are [expected current latest] (= expected (latest? {:version current :latest-version latest}))
    true "3.3.4" "2.3.4"
    true "3.3" "2.3.4"
    true "3" "2.3.4"

    true "2.3.4" "2.3.4"
    true "2.3" "2.3.4"
    true "2" "2.3.4"

    false "1.3.4" "2.3.4"
    false "1.3" "2.3.4"
    false "1" "2.3.4"

    false "2.2.4" "2.3.4"
    false "2.2" "2.3.4"

    false "2.3.3" "2.3.4"))
