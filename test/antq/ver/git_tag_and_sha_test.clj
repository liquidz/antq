(ns antq.ver.git-tag-and-sha-test
  (:require
   [antq.record :as r]
   [antq.util.git :as u.git]
   [antq.ver :as ver]
   [antq.ver.git-tag-and-sha]
   [clojure.test :as t]))

(defn- dep
  [m]
  (r/map->Dependency (merge {:type :git-tag-and-sha} m)))

(t/deftest get-sorted-versions-test
  (with-redefs [u.git/tags-by-ls-remote (constantly ["v2.0.0"
                                                     "invalid"
                                                     "v1.0.0"])]
    (t/is (= ["v2.0.0" "v1.0.0"]
             (ver/get-sorted-versions (dep {:extra {:url "dummy"}})))))

  (t/testing "url is nil"
    (t/is (empty? (ver/get-sorted-versions (dep {}))))))
