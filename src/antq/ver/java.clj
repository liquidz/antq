(ns antq.ver.java
  (:require
   [antq.ver :as ver]
   [clojure.string :as str]
   [clojure.tools.deps.alpha.util.maven :as deps.util.maven]
   [clojure.tools.deps.alpha.util.session :as deps.util.session]
   [version-clj.core :as version])
  (:import
   (org.eclipse.aether
    RepositorySystem
    RepositorySystemSession)
   (org.eclipse.aether.resolution
    VersionRangeRequest)))

(def default-repos
  {"central" {:url "https://repo1.maven.org/maven2/"}
   "clojars" {:url "https://repo.clojars.org/"}})

(defn- normalize-repo-url
  [url]
  (-> url
      (str/replace #"^s3p://" "s3://")))

(defn normalize-repos
  [repos]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (if (contains? v :url)
                    (update v :url normalize-repo-url)
                    v)))
   {} repos))

(defn get-versions
  [name opts]
  (let [lib (cond-> name (string? name) symbol)
        local-repo deps.util.maven/default-local-repo
        system ^RepositorySystem (deps.util.session/retrieve :mvn/system #(deps.util.maven/make-system))
        session ^RepositorySystemSession (deps.util.session/retrieve :mvn/session #(deps.util.maven/make-session system local-repo))
        ; c.f. https://stackoverflow.com/questions/35488167/how-can-you-find-the-latest-version-of-a-maven-artifact-from-java-using-aether
        artifact (deps.util.maven/coord->artifact lib {:mvn/version "[0,)"})
        remote-repos (deps.util.maven/remote-repos (:repositories opts))
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
  [dep]
  (get-sorted-versions-by-name
   (:name dep)
   {:repositories (-> default-repos
                      (merge (:repositories dep))
                      (normalize-repos))
    :snapshots? (ver/snapshot? (:version dep))}))
