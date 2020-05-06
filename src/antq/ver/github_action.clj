(ns antq.ver.github-action
  (:require
   [antq.util.ver :as u.ver]
   [antq.util.xml :as u.xml]
   [antq.ver :as ver]
   [clojure.data.xml :as xml]
   [version-clj.core :as version]))

(defn releases-atom [dep]
  (format "https://github.com/%s/releases.atom" (:name dep)))

(defn get-latest-version-by-url*
  [url]
  (->> url
       slurp
       xml/parse-str
       xml-seq
       (filter (comp #{:entry} :tag))
       (map #(u.ver/normalize-version (u.xml/get-value (:content %) :title)))
       (filter u.ver/sem-ver?)
       (sort version/version-compare)
       last))

(def get-latest-version-by-url
  (memoize get-latest-version-by-url*))

(defmethod ver/get-latest-version :github-action
  [dep]
  (-> dep releases-atom get-latest-version-by-url))
