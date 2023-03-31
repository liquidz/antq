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

(t/deftest detect-project-test
  (t/are [expected file-path] (= expected (sut/detect-project file-path))
    :clojure "/path/to/deps.edn"
    :clojure "/path/to/bb.edn"
    :boot "/path/to/build.boot"
    :gradle "/path/to/build.gradle"
    :leiningen "/path/to/project.clj"
    :shadow-cljs "/path/to/shadow-cljs.edn"
    ::sut/unknown "/path/to/invalid"))
