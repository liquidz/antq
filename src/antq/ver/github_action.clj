(ns antq.ver.github-action
  (:require
   [antq.util.ver :as u.ver]
   [antq.ver :as ver]
   [cheshire.core :as json]
   [clojure.string :as str]
   [version-clj.core :as version])
  (:import
   java.io.PrintWriter))

(defn tag-api-url
  [dep]
  (format "https://api.github.com/repos/%s/tags"
          (str/join "/" (take 2 (str/split (:name dep) #"/")))))

(defn get-sorted-versions-by-url*
  [url]
  (try
    (-> url
        slurp
        (json/parse-string true)
        (->> (map (comp u.ver/normalize-version :name))
             (filter u.ver/sem-ver?)
             (sort version/version-compare)
             reverse))
    (catch Exception ex
      (.println ^PrintWriter *err* (str "Failed to fetch versions from GitHub: " (.getMessage ex)))
      [])))

(def get-sorted-versions-by-url
  (memoize get-sorted-versions-by-url*))

(defmethod ver/get-sorted-versions :github-action
  [dep]
  (-> dep tag-api-url get-sorted-versions-by-url))

(defn- nth-newer?
  [current-ver-seq latest-ver-seq index]
  (let [current (nth (first current-ver-seq) index nil)
        latest (nth (first latest-ver-seq) index nil)]
    (and current latest
         (>= current latest))))

(defmethod ver/latest? :github-action
  [dep]
  (let [current (some-> dep :version version/version->seq)
        latest (some-> dep :latest-version version/version->seq)]
    (when (and current latest)
      (case (count (first current))
        1 (nth-newer? current latest 0)
        2 (and (nth-newer? current latest 0)
               (nth-newer? current latest 1))
        (<= 0 (version/version-seq-compare current latest))))))
