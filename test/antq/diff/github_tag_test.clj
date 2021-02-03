(ns antq.diff.github-tag-test
  (:require
   [antq.diff :as diff]
   [antq.diff.github-tag]
   [antq.record :as r]
   [clojure.test :as t]))

(t/deftest get-diff-url-test
  (let [dep (r/map->Dependency {:type :github-tag
                                :name "foo/bar"
                                :version "1.0"
                                :latest-version "2.0"})]
    (t/is (= "https://github.com/foo/bar/compare/1.0...2.0"
             (diff/get-diff-url dep)))))
