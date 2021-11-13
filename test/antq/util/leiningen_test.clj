(ns antq.util.leiningen-test
  (:require
   [antq.util.env :as u.env]
   [antq.util.leiningen :as sut]
   [clojure.test :as t]))

(t/deftest env-test
  (with-redefs [u.env/getenv identity]
    (t/is (= "LEIN_PASSWORD" (sut/env :env)))
    (t/is (= "FOO" (sut/env :env/foo)))
    (t/is (= "FOO_BAR" (sut/env :env/foo_bar)))
    (t/is (= nil (sut/env :invalid/foo_bar)))
    (t/is (= nil (sut/env "string")))))
