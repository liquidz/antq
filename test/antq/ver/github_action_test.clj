(ns antq.ver.github-action-test
  (:require
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.github-action :as sut]
   [cheshire.core :as json]
   [clojure.test :as t]))

(t/deftest tag-api-url-test
  (t/are [expected in] (= expected (sut/tag-api-url {:name in}))
    "https://api.github.com/repos/foo/bar/tags" "foo/bar"
    "https://api.github.com/repos/foo/bar/tags" "foo/bar/baz"))

(defn- get-sorted-versions
  [m]
  (ver/get-sorted-versions (r/map->Dependency (assoc m :type :github-action :version "1.0.0"))))

(def ^:private dummy-json
  (json/generate-string
   [{:name "v1.0.0"}
    {:name "v2.0.0"}
    {:name "v3.0.0"}]))

(t/deftest get-sorted-version-test
  (with-redefs [slurp (constantly dummy-json)]
    (t/is (= ["3.0.0" "2.0.0" "1.0.0"]
             (get-sorted-versions {:name "foo/bar"})))))
