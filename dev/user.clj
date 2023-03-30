(ns user
  (:require
   [clojure.pprint :as pp]
   [malli.dev :as m.dev]))

(defn go
  []
  (try
    (m.dev/stop!)
    (m.dev/start!)
    (catch clojure.lang.ExceptionInfo ex
      (println (ex-message ex))
      (pp/pprint (ex-data ex)))))
