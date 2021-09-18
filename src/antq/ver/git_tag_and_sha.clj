(ns antq.ver.git-tag-and-sha
  (:require
   [antq.util.git :as u.git]
   [antq.util.ver :as u.ver]
   [antq.ver :as ver]
   [version-clj.core :as version]))

(defmethod ver/get-sorted-versions :git-tag-and-sha
  [dep]
  (or (some->> (get-in dep [:extra :url])
               (u.git/tags-by-ls-remote)
               (filter (comp u.ver/sem-ver? u.ver/normalize-version))
               (sort (fn [& args]
                       (apply version/version-compare
                              (map u.ver/normalize-version args))))
               (reverse))
      []))
