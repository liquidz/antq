(ns antq.util.git-test
  (:require
   [antq.util.git :as sut]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.test :as t]))

(def ^:private dummy-ls-remote-out
  (->> [["dummy-sha" "HEAD"]
        ["dummy-sha" "refs/heads/foo"]
        ["dummy-sha" "refs/pull/1/head"]
        ["dummy-sha" "refs/tags/v1"]
        ["dummy-sha" "refs/tags/v2"]]
       (map #(str/join "\t" %))
       (str/join "\n")))

(t/deftest tags-by-ls-remote*-test
  (with-redefs [sh/sh (constantly {:exit 0
                                   :out dummy-ls-remote-out
                                   :err ""})]
    (t/is (= ["v1" "v2"]
             (sut/tags-by-ls-remote* "dummy url")))))

