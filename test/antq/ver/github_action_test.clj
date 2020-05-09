(ns antq.ver.github-action-test
  (:require
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.github-action :as sut]
   [cheshire.core :as json]
   [clojure.test :as t]))

(defn- dep
  [m]
  (r/map->Dependency (merge {:type :github-action} m)))

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

(t/deftest get-sorted-version-test
  (with-redefs [slurp (constantly dummy-json)]
    (t/is (= ["3.0.0" "2.0.0" "1.0.0"]
             (get-sorted-versions {:name "foo/bar"})))))

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
