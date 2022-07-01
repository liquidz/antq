(ns antq.util.ver
  (:require
   [antq.util.exception :as u.ex]
   [clojure.string :as str]))

(def ^:private no-latest-version-error
  "Failed to fetch")

(def ^:private timed-out-error
  "Timed out")

(defn remove-qualifiers
  "c.f. https://github.com/xsc/version-clj/blob/v2.0.2/src/version_clj/qualifiers.cljc"
  [s]
  (str/replace s #"[\-.](alpha|beta|milestone|rc|snapshot|final|stable)\d*" ""))

(defn normalize-version
  [s]
  (str/replace s #"^[^\d]+" ""))

(defn sem-ver?
  [s]
  (some? (re-find #"^\d+(\.\d+)*$" s)))

(defmulti normalize-latest-version
  (fn [dep] (:type dep)))

(defmethod normalize-latest-version :default
  [{:keys [latest-version]}]
  (cond
    (string? latest-version)
    latest-version

    (u.ex/ex-timeout? latest-version)
    timed-out-error

    :else
    no-latest-version-error))

(defmethod normalize-latest-version :git-sha
  [{:keys [version latest-version]}]
  (cond
    (and version
         latest-version
         (string? version)
         (string? latest-version))
    (subs latest-version 0 (count version))

    (u.ex/ex-timeout? latest-version)
    timed-out-error

    :else
    no-latest-version-error))
