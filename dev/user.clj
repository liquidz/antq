(ns user
  (:require
   [malli.dev :as m.dev]))

(defn go
  []
  (m.dev/stop!)
  (m.dev/start!))
