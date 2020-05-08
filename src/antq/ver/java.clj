(ns antq.ver.java
  (:require
   [ancient-clj.core :as ancient]
   [antq.ver :as ver]))

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn get-sorted-versions-by-name*
  [name opts]
  (map :version-string
       (ancient/versions! name opts)))

(def get-sorted-versions-by-name
  (memoize get-sorted-versions-by-name*))

(defmethod ver/get-sorted-versions :java
  [dep]
  (get-sorted-versions-by-name
   (:name dep)
   {:repositories default-repos
    :snapshots? (ver/snapshot? (:version dep))}))
