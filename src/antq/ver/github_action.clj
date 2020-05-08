(ns antq.ver.github-action
  (:require
   [antq.util.ver :as u.ver]
   [antq.util.xml :as u.xml]
   [antq.ver :as ver]
   [clojure.data.xml :as xml]
   [clojure.string :as str]
   [version-clj.core :as version]))

(defn releases-atom
  [dep]
  (format "https://github.com/%s/releases.atom"
          (str/join "/" (take 2 (str/split (:name dep) #"/")))))

(defn get-sorted-versions-by-url*
  [url]
  (->> url
       slurp
       xml/parse-str
       xml-seq
       (filter (comp #{:entry} :tag))
       (map #(u.xml/get-attribute (:content %) :link :href))
       (map #(u.ver/normalize-version (last (str/split % #"/"))))
       (filter u.ver/sem-ver?)
       (sort version/version-compare)
       reverse))

(def get-sorted-versions-by-url
  (memoize get-sorted-versions-by-url*))

(defmethod ver/get-sorted-versions :github-action
  [dep]
  (-> dep releases-atom get-sorted-versions-by-url))
