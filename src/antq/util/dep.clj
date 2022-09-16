(ns antq.util.dep
  (:require
   [antq.util.function :as u.fn]
   [antq.util.maven :as u.mvn]
   [antq.util.url :as u.url]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   java.io.File
   (org.eclipse.aether
    DefaultRepositorySystemSession
    RepositorySystem)
   (org.eclipse.aether.artifact
    Artifact)
   (org.eclipse.aether.repository
    RemoteRepository)
   (org.eclipse.aether.resolution
    ArtifactRequest)))

(defn compare-deps
  [x y]
  (if (and (string? (:file x))
           (string? (:file y)))
    (let [prj (.compareTo ^String (:file x) ^String (:file y))]
      (if (zero? prj)
        (.compareTo ^String (:name x) ^String (:name y))
        prj))
    0))

(defn relative-path
  [^File target-file]
  (-> (.getPath target-file)
      (str/replace-first #"^\./" "")))

(defn name-candidates
  [^String dep-name]
  (let [[group-id artifact-id] (str/split dep-name #"/" 2)
        candidates (cond-> #{}
                     (seq dep-name) (conj (symbol dep-name)))]
    (cond-> candidates
      (= group-id artifact-id) (conj (symbol group-id)))))

(defn repository-opts
  [dep]
  {:repositories (-> u.mvn/default-repos
                     (merge (:repositories dep))
                     (u.mvn/normalize-repos))
   :snapshots? (u.mvn/snapshot? (:version dep))})

(defmulti normalize-version-by-name
  (fn [dep] (:name dep)))

(defmethod normalize-version-by-name :default
  [dep]
  dep)

(defn normalize-path
  [^String path]
  (let [file (io/file path)]
    (try
      (let [path' (-> file
                      (.toPath)
                      (.normalize)
                      (str))]
        (if (and (not (str/blank? path))
                 (str/blank? path'))
          "."
          path'))
      (catch Exception _
        (.getCanonicalPath file)))))

(defn- get-repository-url*
  [{:keys [name version] :as dep}]
  (try
    (let [opts (repository-opts dep)
          {:keys [^RepositorySystem system
                  ^DefaultRepositorySystemSession  session
                  ^Artifact artifact
                  remote-repos]} (u.mvn/repository-system name version opts)
          req (doto (ArtifactRequest.)
                (.setArtifact artifact)
                (.setRepositories remote-repos))
          repo (some-> (.resolveArtifact system session req)
                       (.getRepository))]
      ;; repo may be org.eclipse.aether.repository.LocalRepository
      (when (instance? RemoteRepository repo)
        (.getUrl ^RemoteRepository repo)))
    ;; Skip showing diff URL when fetching repository URL is failed
    (catch Exception _ nil)))
(def get-repository-url (u.fn/memoize-by get-repository-url* :name))

(defn- dep->pom-url
  [dep]
  (let [{:keys [version]} dep
        [group-id artifact-id] (str/split (:name dep) #"/" 2)
        repo-url (get-repository-url dep)]
    (when repo-url
      (format "%s%s/%s/%s/%s-%s.pom"
              (u.url/ensure-tail-slash repo-url)
              (str/replace group-id "." "/")
              artifact-id
              version
              artifact-id
              version))))

(defn- get-scm-url*
  [dep]
  (try
    (when-let [model (some-> dep
                             (dep->pom-url)
                             (u.mvn/read-pom))]
      (let [scm-url (some-> model
                            (u.mvn/get-model-scm)
                            (u.mvn/get-scm-url))
            project-url (u.mvn/get-model-url model)]
        (some-> (or scm-url project-url)
                (u.url/ensure-https)
                (u.url/ensure-git-https-url))))
    ;; Skip showing diff URL when POM file is not found
    (catch java.io.FileNotFoundException _ nil)))
(def get-scm-url (u.fn/memoize-by get-scm-url* :name))
