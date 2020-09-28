(ns antq.report.edn-test
  (:require
   [antq.report :as report]
   [antq.report.edn]
   [clojure.string :as str]
   [clojure.test :as t]))

(t/deftest edn-reporter-test
  (t/is (= "({:foo \"bar\"})"
           (str/trim
            (with-out-str
              (report/reporter [{:foo "bar"}] {:reporter "edn"}))))))
