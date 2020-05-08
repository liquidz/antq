(ns antq.ver.java
  (:require
   [ancient-clj.core :as ancient]
   [antq.ver :as ver]))

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn get-sorted-versions-by-name*
  [name]
  (map :version-string
       (ancient/versions! name {:repositories default-repos
                                :snapshots? false})))
(def get-sorted-versions-by-name
  (memoize get-sorted-versions-by-name*))

(defmethod ver/get-sorted-versions :java
  [dep]
  (-> dep :name get-sorted-versions-by-name))

