(ns antq.test-helper
  (:require
   [antq.record :as r]))

(defn test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))
