(ns antq.diff.java
  (:require
   [antq.diff :as diff]
   [antq.log :as log]
   [antq.util.git :as u.git]
   [antq.util.maven :as u.mvn]
   [antq.util.url :as u.url]
   [clojure.string :as str])
  (:import
   (org.eclipse.aether
    DefaultRepositorySystemSession
    RepositorySystem)
   (org.eclipse.aether.artifact
    Artifact)
   (org.eclipse.aether.repository
    RemoteRepository)
   (org.eclipse.aether.resolution
    ArtifactRequest)))

(defn memoize-by
  [f key-fn]
  (let [mem (atom {})]
    (fn [m & args]
      (if-let [res (get @mem (get m key-fn))]
        res
        (let [ret (apply f m args)]
          (swap! mem assoc (get m key-fn) ret)
          ret)))))

(defn- get-repository-url*
  [{:keys [name version] :as dep}]
  (try
    (let [opts (u.mvn/dep->opts dep)
          {:keys [^RepositorySystem system
                  ^DefaultRepositorySystemSession  session
                  ^Artifact artifact
                  remote-repos]} (u.mvn/repository-system name version opts)
          req (doto (ArtifactRequest.)
                (.setArtifact artifact)
                (.setRepositories remote-repos))]
      (some-> (.resolveArtifact system session req)
              ^RemoteRepository (.getRepository)
              (.getUrl)))
    ;; Skip showing diff URL when fetching repository URL is failed
    (catch Exception _ nil)))
(def get-repository-url (memoize-by get-repository-url* :name))

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
                            (u.mvn/get-scm)
                            (u.mvn/get-scm-url))
            project-url (u.mvn/get-url model)]
        (some-> (or scm-url project-url)
                (u.url/ensure-https)
                (u.url/ensure-git-https-url))))
    ;; Skip showing diff URL when POM file is not found
    (catch java.io.FileNotFoundException _ nil)))
(def get-scm-url (memoize-by get-scm-url* :name))

(defmethod diff/get-diff-url :java
  [{:as dep :keys [version latest-version]}]
  (when (and version latest-version)
    (when-let [url (get-scm-url dep)]
      (cond
        (str/starts-with? url "https://github.com/")
        (let [tags (u.git/tags-by-ls-remote url)
              current (first (filter #(str/includes? % version) tags))
              latest (or (first (filter #(str/includes? % latest-version) tags))
                         ;; If there isn't a tag for latest version
                         "head")]
          (if current
            (format "%scompare/%s...%s"
                    (u.url/ensure-tail-slash url)
                    current
                    latest)
            (do (log/warning (str "The tag for current version is not found: " url))
                ;; not diff, but URL is useful for finding the differences.
                nil)))

        :else
        (do (log/warning (str "Diff is not supported for " url))
            ;; not diff, but URL is useful for finding the differences.
            nil)))))
