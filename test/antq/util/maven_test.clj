(ns antq.util.maven-test
  (:require
   [antq.record :as r]
   [antq.util.env :as u.env]
   [antq.util.maven :as sut]
   [clojure.test :as t]
   [clojure.tools.deps.alpha.util.maven :as deps.util.maven])
  (:import
   (org.apache.maven.settings
    Server
    Settings)))

(def ^:private dummy-settings
  (doto (Settings.)
    (.addServer (doto (Server.)
                  (.setId "serv1")))
    (.addServer (doto (Server.)
                  (.setId "serv2")
                  (.setUsername "two-user")
                  (.setPassword "two-pass")))))

(def ^:private dummy-repos
  {"serv1" {:url "https://one.example.com"}
   "serv2" {:url "https://two.example.com"}
   "serv3" {:url "https://three.example.com"
            :username "three-user"
            :password "three-pass"}
   "serv4" {:url "https://three.example.com"
            :username :env
            :password :env/four}})

(def ^:private dummy-env
  {"LEIN_PASSWORD" "lein-pass"
   "FOUR" "env-four"})

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

(t/deftest get-maven-settings-test
  (with-redefs [deps.util.maven/get-settings (constantly dummy-settings)
                u.env/getenv #(get dummy-env %)]
    (let [settings (sut/get-maven-settings {:repositories dummy-repos})
          servers (map #(hash-map
                         :id (.getId %)
                         :username (.getUsername %)
                         :password (.getPassword %))
                       (.getServers settings))]
      (t/is (= 4 (count servers)))

      (t/is (= #{{:id "serv1" :username nil :password nil}
                 ;; from settings.xml
                 {:id "serv2" :username "two-user" :password "two-pass"}
                 ;; from project.clj
                 {:id "serv3" :username "three-user" :password "three-pass"}
                 ;; from project.clj with environmental variable
                 {:id "serv4" :username "lein-pass" :password "env-four"}}
               (set servers))))))

(t/deftest get-url-test
  (let [model (sut/read-pom "pom.xml")]
    (t/is (= "https://github.com/liquidz/antq"
             (sut/get-url model)))))

(t/deftest get-scm-url-test
  (let [model (sut/read-pom "pom.xml")
        scm (sut/get-scm model)]
    (t/is (= "https://github.com/liquidz/antq"
             (sut/get-scm-url scm)))))
