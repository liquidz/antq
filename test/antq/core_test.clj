(ns antq.core-test
  (:require
   [antq.core :as sut]
   [clojure.test :as t]))

(t/deftest greet-test
  (t/is (= (sut/greet "world") "hello world")))
