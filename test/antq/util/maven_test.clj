(ns antq.util.maven-test
  (:require
   [antq.record :as r]
   [antq.util.maven :as sut]
   [clojure.test :as t]))

(t/deftest normalize-repo-url-test
  (t/are [expected in] (= expected (sut/normalize-repo-url in))
    "" ""
    "foo" "foo"
    "s3://foo/bar" "s3p://foo/bar"))

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

(t/deftest snapshot?-test
  (t/are [expected in] (= expected (sut/snapshot? in))
    false ""
    false "foo"
    true "foo-snapshot"
    true "foo-SnapShot"
    true "foo-SNAPSHOT"))

(t/deftest dep->opts-test
  (t/is (= {:repositories sut/default-repos
            :snapshots? false}
           (sut/dep->opts (r/map->Dependency {:version "1.0.0"}))))
  (t/is (= {:repositories (assoc sut/default-repos
                                 "foo" {:url "s3://foo"})
            :snapshots? true}
           (sut/dep->opts (r/map->Dependency {:repositories {"foo" {:url "s3p://foo"}}
                                              :version "1.0.0-SNAPSHOT"})))))

(t/deftest get-scm-url-test
  (let [model (sut/read-pom "pom.xml")
        scm (sut/get-scm model)]
    (t/is (= "https://github.com/liquidz/antq"
             (sut/get-scm-url scm)))))
