(ns antq.ver.java-test
  (:require
   [antq.record :as r]
   [antq.ver :as ver]
   [antq.ver.java :as sut]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.test :as t]))

(def ^:private current-clojure-version
  (get-in (edn/read-string (slurp "deps.edn"))
          [:deps 'org.clojure/clojure :mvn/version]))

(t/deftest normalize-repos-test
  (t/is (= sut/default-repos
           (sut/normalize-repos sut/default-repos)))
  (t/is (= {"foo" {:url "s3://bar"}}
           (sut/normalize-repos {"foo" {:url "s3://bar"}})))

  (t/is (= {"foo" {:invalid "invalid"}}
           (sut/normalize-repos {"foo" {:invalid "invalid"}})))

  (t/testing "replace s3p:// to s3://"
    (t/is (= {"foo" {:url "s3://bar"}}
             (sut/normalize-repos {"foo" {:url "s3p://bar"}})))
    (t/is (= {"foo" {:url "s3://bar" :no-auth true}}
             (sut/normalize-repos {"foo" {:url "s3p://bar" :no-auth true}})))))

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
             (get-sorted-versions {:version "1.0.0-snapshot"}))))

  (t/testing "normalizing repository URL"
    (with-redefs [sut/get-sorted-versions-by-name (fn [_ opts] opts)]
      (let [res (get-sorted-versions {:repositories {"foo" {:url "s3p://bar"}}})
            diff (set/difference (set (:repositories res))
                                 (set sut/default-repos))]
        (t/is (= #{["foo" {:url "s3://bar"}]}
                 diff))))))
