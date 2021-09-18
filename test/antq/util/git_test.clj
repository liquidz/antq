(ns antq.util.git-test
  (:require
   [antq.util.git :as sut]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.test :as t])
  (:import
   java.util.UUID))

(def ^:private dummy-ls-remote-tag-out
  (->> [["dummy-sha1" "HEAD"]
        ["dummy-sha2" "refs/heads/foo"]
        ["dummy-sha3" "refs/pull/1/head"]
        ["dummy-sha4" "refs/tags/v1"]
        ["dummy-sha5" "refs/tags/v2"]]
       (map #(str/join "\t" %))
       (str/join "\n")))

(def ^:private dummy-ls-remote-sha-out
  (->> [["foo-sha" "FOO"]
        ["head-sha" "HEAD"]
        ["bar-sha" "BAR"]]
       (map #(str/join "\t" %))
       (str/join "\n")))

(defn- dummy-url []
  (str "dummy url" (UUID/randomUUID)))

(t/deftest tags-by-ls-remote*-test
  (with-redefs [sh/sh (constantly {:exit 0
                                   :out dummy-ls-remote-tag-out
                                   :err ""})]
    (t/is (= ["v1" "v2"]
             (sut/tags-by-ls-remote* (dummy-url))))))

(t/deftest head-sha-by-ls-remote*-test
  (with-redefs [sh/sh (constantly {:exit 0
                                   :out dummy-ls-remote-sha-out
                                   :err ""})]
    (t/is (= "head-sha"
             (sut/head-sha-by-ls-remote* (dummy-url))))))

(t/deftest tag-sha-by-ls-remote*-test
  (with-redefs [sh/sh (constantly {:exit 0
                                   :out dummy-ls-remote-tag-out
                                   :err ""})]
    (t/is (= "dummy-sha4"
             (sut/tag-sha-by-ls-remote* (dummy-url) "v1")))))
