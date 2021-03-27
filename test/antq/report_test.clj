(ns antq.report-test
  (:require
   [antq.report :as sut]
   [clojure.test :as t]))

(t/deftest unknown-reporter-test
  (t/is (nil? (sut/reporter {} {:reporter :unknown-reporter}))))
