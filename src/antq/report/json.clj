(ns antq.report.json
  (:require
   [antq.report :as report]
   [clojure.data.json :as json]))

(defmethod report/reporter "json"
  [deps _options]
  (println (json/write-str deps)))
