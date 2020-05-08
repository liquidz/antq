(ns antq.ver.github-action-test
  (:require
   [antq.ver.github-action :as sut]
   [clojure.test :as t]))

(t/deftest tag-api-url-test
  (t/are [expected in] (= expected (sut/tag-api-url {:name in}))
    "https://api.github.com/repos/foo/bar/tags" "foo/bar"
    "https://api.github.com/repos/foo/bar/tags" "foo/bar/baz"))
