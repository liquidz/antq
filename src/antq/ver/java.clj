(ns antq.ver.java
  (:require
   [antq.util.maven :as u.mvn]
   [antq.ver :as ver]
   [version-clj.core :as version])
  (:import
   (org.eclipse.aether
    DefaultRepositorySystemSession
    RepositorySystem)
   (org.eclipse.aether.artifact
    Artifact)
   (org.eclipse.aether.resolution
    VersionRangeRequest)))

(defn get-versions
  [name opts]
  (let [{:keys [^RepositorySystem system
                ^DefaultRepositorySystemSession  session
                ^Artifact artifact
                remote-repos]} (u.mvn/repository-system name "[0,)" opts)
        req (doto (VersionRangeRequest.)
              (.setArtifact artifact)
              (.setRepositories remote-repos))]
    (->> (.resolveVersionRange system session req)
         (.getVersions))))

(defn get-sorted-versions-by-name*
  [name opts]
  (let [sorted-versions (->> (get-versions name opts)
                             (map str)
                             (sort version/version-compare)
                             (reverse))]
    (cond->> sorted-versions
      (not (:snapshots? opts)) (remove ver/snapshot?))))

(def get-sorted-versions-by-name
  (memoize get-sorted-versions-by-name*))

(defmethod ver/get-sorted-versions :java
  [dep options]
  (get-sorted-versions-by-name (:name dep)
                               (u.mvn/dep->opts dep)
                               options))
