(ns antq.util.ver
  (:require
   [clojure.string :as str]))

(def ^:private no-latest-version-error
  "Failed to fetch")

(defn normalize-version
  [s]
  (str/replace s #"^v" ""))

(defn sem-ver?
  [s]
  (some? (re-find #"^\d+(\.\d+){0,2}$" s)))

(defmulti normalize-latest-version
  (fn [dep] (:type dep)))

(defmethod normalize-latest-version :default
  [{:keys [latest-version]}]
  (or latest-version
      no-latest-version-error))

(defmethod normalize-latest-version :git-sha
  [{:keys [version latest-version]}]
  (if (and version latest-version)
    (subs latest-version 0 (count version))
    no-latest-version-error))
