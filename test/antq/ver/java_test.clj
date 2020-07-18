(ns antq.ver.java-test
  (:require
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.java :as sut]
   [clojure.edn :as edn]
   [clojure.test :as t]))

(def ^:private current-clojure-version
  (get-in (edn/read-string (slurp "deps.edn"))
          [:deps 'org.clojure/clojure :mvn/version]))

(t/deftest get-versions-test
  (let [vers (sut/get-versions 'org.clojure/clojure
                               {:repositories sut/default-repos})]
    (t/is (seq vers))
    (t/is (contains? (set (map str vers)) current-clojure-version))))

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
