(ns antq.report.json-test
  (:require
   [antq.report :as report]
   [antq.report.json]
   [clojure.string :as str]
   [clojure.test :as t]))

(t/deftest json-reporter-test
  (t/is (= "[{\"foo\":\"bar\"}]"
           (str/trim
            (with-out-str
              (report/reporter [{:foo "bar"}] {:reporter "json"}))))))
