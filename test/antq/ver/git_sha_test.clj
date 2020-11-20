(ns antq.ver.git-sha-test
  (:require
   [antq.ver :as ver]
   [antq.ver.git-sha :as sut]
   [clojure.test :as t]))

(t/deftest get-sorted-versions-test
  (with-redefs [sut/git-ls-remote (fn [url]
                                    (when (= url "https://example.com")
                                      {:out  "foo-sha\tFOO\nhead-sha\tHEAD\nbar-sha\tBAR"}))]
    (t/is (= ["head-sha"]
             (ver/get-sorted-versions {:type :git-sha :extra {:url "https://example.com"}})))

    (t/is (= []
             (ver/get-sorted-versions {:type :git-sha :extra {:url "failed to fetch"}})))))

(t/deftest latest?-test
  (t/is (true? (ver/latest? {:type :git-sha :version "foo" :latest-version "foo"})))
  (t/is (false? (ver/latest? {:type :git-sha :version "foo" :latest-version "bar"}))))
