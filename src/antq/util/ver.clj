(ns antq.util.ver
  (:require
   [clojure.string :as str]))

(defn normalize-version
  [s]
  (str/replace s #"^v" ""))

(defn sem-ver?
  [s]
  (some? (re-find #"^\d+(\.\d+){0,2}$" s)))
