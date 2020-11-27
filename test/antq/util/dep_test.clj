(ns antq.util.dep-test
  (:require
   [antq.test-helper :as h]
   [antq.util.dep :as sut]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(t/deftest compare-deps-test
  (let [aaa (h/test-dep {:file "aa" :name "bb"})
        bbb (h/test-dep {:file "aa" :name "xx"})
        ccc (h/test-dep {:file "xx" :name "bb"})
        ddd (h/test-dep {:file "xx" :name "xx"})]
    (t/are [expected-fn x y] (expected-fn (sut/compare-deps x y))
      zero? aaa aaa
      neg?  aaa bbb
      neg?  aaa ccc
      neg?  aaa ddd

      pos?  bbb aaa
      zero? bbb bbb
      neg?  bbb ccc
      neg?  bbb ddd

      pos?  ccc aaa
      pos?  ccc bbb
      zero? ccc ccc
      neg?  ccc ddd

      pos?  ddd aaa
      pos?  ddd bbb
      pos?  ddd ccc
      zero? ddd ddd)))

(t/deftest relative-path-test
  (t/is (= "foo" (sut/relative-path (io/file "foo"))))
  (t/is (= "foo" (sut/relative-path (io/file "." "foo"))))
  (t/is (= "foo" (sut/relative-path (io/file "./foo"))))

  (t/is (= (.getPath (io/file "foo" "bar"))
           (sut/relative-path (io/file "." "foo" "bar"))))

  (t/is (= (.getPath (io/file "foo" "bar"))
           (sut/relative-path (io/file "./foo" "bar")))))

(t/deftest name-candidates-test
  (t/is (= #{'foo/core}
           (sut/name-candidates "foo/core")))
  (t/is (= #{'foo/foo 'foo}
           (sut/name-candidates "foo/foo")))
  (t/is (= #{}
           (sut/name-candidates ""))))
