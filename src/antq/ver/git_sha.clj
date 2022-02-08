(ns antq.ver.git-sha
  (:require
   [antq.util.git :as u.git]
   [antq.ver :as ver]))

(defmethod ver/get-sorted-versions :git-sha
  [dep _options]
  (or (some-> (get-in dep [:extra :url])
              (u.git/head-sha-by-ls-remote)
              (vector))
      []))

(defmethod ver/latest? :git-sha
  [dep]
  (let [current (some-> dep :version)
        latest (some-> dep :latest-version
                       (subs 0 (count current)))]
    (= latest current)))
