(ns antq.util.ver
  (:require
   [clojure.string :as str]))

(def ^:private no-latest-version-error
  "Failed to fetch")

(defn- remove-first-non-digit-chars [s]
  (str/replace s #"^[^\d]+" ""))

(defn- remove-qualifiers
  "c.f. https://github.com/xsc/version-clj/blob/v2.0.2/src/version_clj/qualifiers.cljc"
  [s]
  (str/replace s #"[\-.](alpha|beta|milestone|rc|snapshot|final|stable)\d*" ""))

(def normalize-version
  (comp remove-qualifiers
        remove-first-non-digit-chars))

(defn sem-ver?
  [s]
  (some? (re-find #"^\d+(\.\d+)*$" s)))

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
