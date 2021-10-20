(ns antq.ver.github-tag
  (:require
   [antq.log :as log]
   [antq.util.git :as u.git]
   [antq.util.ver :as u.ver]
   [antq.ver :as ver]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [version-clj.core :as version]))

(defonce ^:private failed-to-fetch-from-api
  (atom false))

(defn tag-api-url
  [dep]
  (format "https://api.github.com/repos/%s/tags"
          (str/join "/" (take 2 (str/split (:name dep) #"/")))))

(defn get-sorted-versions-by-ls-remote*
  [dep]
  (let [url (format "https://github.com/%s"
                    (str/join "/" (take 2 (str/split (:name dep) #"/"))))]
    (->> (u.git/tags-by-ls-remote url)
         (filter (comp u.ver/sem-ver? u.ver/normalize-version))
         (sort (fn [& args]
                 (apply version/version-compare
                        (map u.ver/normalize-version args))))
         (reverse))))

(def get-sorted-versions-by-ls-remote
  (memoize get-sorted-versions-by-ls-remote*))

(defn get-sorted-versions-by-url*
  [url]
  (-> url
      (slurp)
      (json/read-str :key-fn keyword)
      (->> (map :name)
           (filter (comp u.ver/sem-ver? u.ver/normalize-version))
           (sort (fn [& args]
                   (apply version/version-compare
                          (map u.ver/normalize-version args))))
           (reverse))))

(def get-sorted-versions-by-url
  (memoize get-sorted-versions-by-url*))

(defn- fallback-to-ls-remote
  [dep]
  (try
    (get-sorted-versions-by-ls-remote dep)
    (catch Exception ex
      (log/error (str "Failed to fetch versions from GitHub: "
                      (.getMessage ex))))))

(defmethod ver/get-sorted-versions :github-tag
  [dep]
  (if @failed-to-fetch-from-api
    (fallback-to-ls-remote dep)
    (try
      (-> dep
          (tag-api-url)
          (get-sorted-versions-by-url))
      (catch Exception ex
        (reset! failed-to-fetch-from-api true)
        (log/warning (str "Failed to fetch versions from GitHub, so fallback to `git ls-remote`: "
                          (.getMessage ex)))
        (fallback-to-ls-remote dep)))))

(defn- nth-newer?
  [current-ver-seq latest-ver-seq index]
  (let [current (nth (first current-ver-seq) index nil)
        latest (nth (first latest-ver-seq) index nil)]
    (and current latest
         (>= current latest))))

(defmethod ver/latest? :github-tag
  [dep]
  (let [current (some-> dep :version u.ver/normalize-version version/version->seq)
        latest (some-> dep :latest-version u.ver/normalize-version version/version->seq)]
    (try
      (when (and current latest)
        (case (count (first current))
          1 (nth-newer? current latest 0)
          2 (and (nth-newer? current latest 0)
                 (nth-newer? current latest 1))
          (<= 0 (version/version-seq-compare current latest))))
      (catch Throwable e
        (log/error (format "Error determining latest version for GitHub dep %s (current: %s, latest: %s): %s"
                           (pr-str (:name dep))
                           (pr-str (:version dep))
                           (pr-str (:latest-version dep))
                           (.getMessage e)))
        true))))
