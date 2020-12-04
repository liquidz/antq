(ns antq.ver.github-action
  (:require
   [antq.util.ver :as u.ver]
   [antq.ver :as ver]
   [cheshire.core :as json]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [version-clj.core :as version])
  (:import
   java.io.PrintWriter))

(defonce ^:private failed-to-fetch-from-api
  (atom false))

(defn tag-api-url
  [dep]
  (format "https://api.github.com/repos/%s/tags"
          (str/join "/" (take 2 (str/split (:name dep) #"/")))))

(defn- github-ls-remote
  [dep]
  (let [url (format "https://github.com/%s"
                    (str/join "/" (take 2 (str/split (:name dep) #"/"))))]
    (sh/sh "git" "ls-remote" url)))

(defn- extract-tags
  [ls-remote-resp]
  (->> (:out ls-remote-resp)
       (str/split-lines)
       (keep #(second (str/split % #"\t" 2)))
       (filter #(= 0 (.indexOf ^String % "refs/tags")))
       (map #(u.ver/normalize-version (str/replace % #"^refs/tags/" "")))
       (filter u.ver/sem-ver?)
       (sort version/version-compare)
       (reverse)))

(defn get-sorted-versions-by-ls-remote*
  [dep]
  (-> dep
      (github-ls-remote)
      (extract-tags)))

(def get-sorted-versions-by-ls-remote
  (memoize get-sorted-versions-by-ls-remote*))

(defn get-sorted-versions-by-url*
  [url]
  (-> url
      (slurp)
      (json/parse-string true)
      (->> (map (comp u.ver/normalize-version :name))
           (filter u.ver/sem-ver?)
           (sort version/version-compare)
           (reverse))))

(def get-sorted-versions-by-url
  (memoize get-sorted-versions-by-url*))

(defn- fallback-to-ls-remote
  [dep]
  (try
    (get-sorted-versions-by-ls-remote dep)
    (catch Exception ex
      (.println ^PrintWriter *err* (str "Failed to fetch versions from GitHub: "
                                        (.getMessage ex))))))

(defmethod ver/get-sorted-versions :github-action
  [dep]
  (if @failed-to-fetch-from-api
    (fallback-to-ls-remote dep)
    (try
      (-> dep
          (tag-api-url)
          (get-sorted-versions-by-url))
      (catch Exception ex
        (reset! failed-to-fetch-from-api true)
        (.println ^PrintWriter *err* (str "Failed to fetch versions from GitHub, so fallback to `git ls-remote`: " (.getMessage ex)))
        (fallback-to-ls-remote dep)))))

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
