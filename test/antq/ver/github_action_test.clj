(ns antq.ver.github-action-test
  (:require
   [antq.ver.github-action :as sut]
   [clojure.test :as t]))

(t/deftest releases-atom-test
  (t/are [expected in] (= expected (sut/releases-atom {:name in}))
    "https://github.com/foo/bar/releases.atom" "foo/bar"
    "https://github.com/foo/bar/releases.atom" "foo/bar/baz"))
