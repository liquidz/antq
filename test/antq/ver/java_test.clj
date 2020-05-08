(ns antq.ver.java-test
  (:require
   [ancient-clj.core :as ancient]
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.java]
   [clojure.test :as t]))

(defn- dummy-versions
  [_ opts]
  (concat
   [{:version-string "1"}]
   (when (contains? (:repositories opts) "dummy")
     [{:version-string "1.3"}])
   (when (:snapshots? opts)
     [{:version-string "1.6"}])
   [{:version-string "2"}]))

(defn- get-sorted-versions
  [m]
  (ver/get-sorted-versions (r/map->Dependency (assoc m :type :java :name "dummy"))))

(t/deftest get-sorted-versions-test
  (with-redefs [ancient/versions! dummy-versions]
    (t/is (= ["1" "2"]
             (get-sorted-versions {:version "1.0.0"})))
    (t/is (= ["1" "1.6" "2"]
             (get-sorted-versions {:version "1.0.0-SNAPSHOT"})))
    (t/is (= ["1" "1.6" "2"]
             (get-sorted-versions {:version "1.0.0-snapshot"})))))
