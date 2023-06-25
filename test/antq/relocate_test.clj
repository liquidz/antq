(ns antq.relocate-test
  (:require
   [antq.relocate :as sut]
   [clojure.test :as t]))

(t/deftest relocated-deps-test
  (t/testing "unverified"
    (with-redefs [sut/get-relocated-deps-name (constantly nil)]
      (let [dummy-deps [{:type :java :name "antq/antq" :latest-version "1"}
                        {:type :java :name "seancorfield/next.jdbc" :latest-version "2"}
                        {:type :java :name "dummy/dummy" :latest-version "3"}
                        {:type :UNKNOWN :name "antq/antq" :latest-version "4"}]]
        (t/is (= [{:type :java
                   :name "antq/antq"
                   :version "antq/antq"
                   :latest-version "1"
                   :latest-name "com.github.liquidz/antq"}
                  {:type :java
                   :name "seancorfield/next.jdbc"
                   :version "seancorfield/next.jdbc"
                   :latest-version "2"
                   :latest-name "com.github.seancorfield/next.jdbc"}]
                 (sut/relocated-deps dummy-deps))))))

  (t/testing "relocated"
    (with-redefs [sut/get-relocated-deps-name (fn [dep]
                                                (when (= "foo/core" (:name dep))
                                                  "new/name"))]
      (let [dummy-deps [{:type :java :name "foo/core" :latest-version "1"}
                        {:type :java :name "bar/core" :latest-version "2"}
                        {:type :UNKNOWN :name "antq/antq" :latest-version "3"}]]
        (t/is (= [{:type :java
                   :name "foo/core"
                   :version "foo/core"
                   :latest-version "1"
                   :latest-name "new/name"}]
                 (sut/relocated-deps dummy-deps)))))))
