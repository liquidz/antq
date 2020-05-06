(ns antq.ver.java
  (:require
   [ancient-clj.core :as ancient]
   [antq.ver :as ver]))

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn get-latest-version-by-name*
  [name]
  (ancient/latest-version-string!
   name
   {:repositories default-repos
    :snapshots? false}))

(def get-latest-version-by-name
  (memoize get-latest-version-by-name*))

(defmethod ver/get-latest-version :java
  [dep]
  (-> dep :name get-latest-version-by-name))
