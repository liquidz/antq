(ns antq.util.file-test
  (:require
   [antq.util.env :as u.env]
   [antq.util.file :as sut]
   [clojure.test :as t]))

(t/deftest normalize-path-test
  (t/testing "HOME"
    (with-redefs [u.env/getenv {"HOME" "/home/foo"}]
      (t/is (= "/path/to/bar" (sut/normalize-path "/path/to/bar")))
      (t/is (= "~/bar" (sut/normalize-path "/home/foo/bar")))))

  (t/testing "Redundant path"
    (t/is (= "/path/to/bar" (sut/normalize-path "/path/to/./foo/../bar"))))

  (t/testing "HOME and Redundant path"
    (with-redefs [u.env/getenv {"HOME" "/home/foo"}]
      (t/is (= "~/bar" (sut/normalize-path "/home/./bar/../foo/bar"))))))
