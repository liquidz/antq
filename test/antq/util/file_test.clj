(ns antq.util.file-test
  (:require
   [antq.util.env :as u.env]
   [antq.util.file :as sut]
   [clojure.test :as t]))

(t/deftest normalize-path-test
  (with-redefs [u.env/getenv {"HOME" "/home/foo"}]
    (t/is (= "/path/to/bar" (sut/normalize-path "/path/to/bar")))
    (t/is (= "~/bar" (sut/normalize-path "/home/foo/bar")))))
