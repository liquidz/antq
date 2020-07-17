(ns antq.ver.java-test
  (:require
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.java :as sut]
   [clojure.test :as t]))

(defn- dummy-versions
  [_ opts]
  (concat
   ["1"]
   (when (contains? (:repositories opts) "dummy")
     ["1.3"])
   ["1.6-SNAPSHOT" "2"]))

(defn- get-sorted-versions
  [m]
  (ver/get-sorted-versions (r/map->Dependency (assoc m :type :java :name "dummy"))))

(t/deftest get-sorted-versions-test
  (with-redefs [sut/get-versions dummy-versions]
    (t/is (= ["2" "1"]
             (get-sorted-versions {:version "1.0.0"})))
    (t/is (= ["2" "1.6-SNAPSHOT" "1"]
             (get-sorted-versions {:version "1.0.0-SNAPSHOT"})))
    (t/is (= ["2" "1.6-SNAPSHOT" "1"]
             (get-sorted-versions {:version "1.0.0-snapshot"})))))
