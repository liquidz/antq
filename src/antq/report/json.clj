(ns antq.report.json
  (:require
   [antq.report :as report]
   [cheshire.core :as json]))

(defmethod report/reporter "json"
  [deps _options]
  (println (json/generate-string deps)))
