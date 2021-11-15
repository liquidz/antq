(ns antq.util.maven
  (:require
   [antq.constant :as const]
   [antq.log :as log]
   [antq.util.leiningen :as u.lein]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps.alpha.util.maven :as deps.util.maven]
   [clojure.tools.deps.alpha.util.session :as deps.util.session])
  (:import
   (org.apache.maven.model
    Model
    Scm)
   org.apache.maven.model.io.xpp3.MavenXpp3Reader
   (org.apache.maven.settings
    Server
    Settings)
   (org.eclipse.aether
    DefaultRepositorySystemSession
    RepositorySystem)
   (org.eclipse.aether.transfer
    TransferEvent
    TransferListener)))

(def default-repos
  {"central" {:url "https://repo1.maven.org/maven2/"}
   "clojars" {:url "https://repo.clojars.org/"}})

(defn normalize-repo-url
  "c.f. https://clojure.org/reference/deps_and_cli#_maven_s3_repos"
  [url]
  (str/replace url #"^s3p://" "s3://"))

(defn normalize-repos
  [repos]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (if (contains? v :url)
                    (update v :url normalize-repo-url)
                    v)))
   {} repos))

(defn snapshot?
  [s]
  (if s
    (str/includes? (str/lower-case s) "snapshot")
    false))

(defn dep->opts
  [dep]
  {:repositories (-> default-repos
                     (merge (:repositories dep))
                     (normalize-repos))
   :snapshots? (snapshot? (:version dep))})

(defn ensure-username-or-password
  [x]
  (if (string? x)
    x
    (or (u.lein/env x)
        (str x))))

(defn- ^Server new-repository-server
  [{:keys [id username password]}]
  (doto (Server.)
    (.setId id)
    (.setUsername (ensure-username-or-password username))
    (.setPassword (ensure-username-or-password password))))

(defn ^Settings get-maven-settings
  [opts]
  (let [settings ^Settings (deps.util.maven/get-settings)
        server-ids (set (map #(.getId %) (.getServers settings)))]
    ;; NOTE
    ;; In Leiningen, authentication information is defined in project.clj instead of ~/.m2/settings.xml,
    ;; so if there is authentication information in `:repositories`, apply to `settings`
    (doseq [[id {:keys [username password]}] (:repositories opts)]
      (when (and username
                 password
                 (not (contains? server-ids id)))
        (.addServer settings
                    (new-repository-server {:id id :username username :password password}))))
    settings))

(def ^TransferListener custom-transfer-listener
  "Copy from clojure.tools.deps.alpha.util.maven/console-listener
  But no outputs for `transferStarted`"
  (reify TransferListener
    (transferStarted [_ event])
    (transferCorrupted [_ event]
      (log/warning "Download corrupted:" (.. ^TransferEvent event getException getMessage)))
    ;; This happens when Maven can't find an artifact in a particular repo
    ;; (but still may find it in a different repo), ie this is a common event
    (transferFailed [_ event])
    (transferInitiated [_ _event])
    (transferProgressed [_ _event])
    (transferSucceeded [_ _event])))

(defn repository-system
  [name version opts]
  (let [lib (cond-> name (string? name) symbol)
        local-repo deps.util.maven/default-local-repo
        system ^RepositorySystem (deps.util.session/retrieve :mvn/system #(deps.util.maven/make-system))
        settings ^Settings (get-maven-settings opts)
        session ^DefaultRepositorySystemSession (deps.util.maven/make-session system settings local-repo)
        ;; Overwrite TransferListener not to show "Downloading" messages
        _ (.setTransferListener session custom-transfer-listener)
        ;; c.f. https://stackoverflow.com/questions/35488167/how-can-you-find-the-latest-version-of-a-maven-artifact-from-java-using-aether
        artifact (deps.util.maven/coord->artifact lib {:mvn/version version})
        remote-repos (deps.util.maven/remote-repos (:repositories opts) settings)]
    {:system system
     :session session
     :artifact artifact
     :remote-repos remote-repos}))

(defn- ^Model read-pom*
  [^String url]
  (with-open [reader (io/reader url)]
    (.read (MavenXpp3Reader.) reader)))

(defn ^Model read-pom
  [^String url]
  (loop [i 0]
    (when (< i const/retry-limit)
      (or (try
            (read-pom* url)
            (catch java.net.ConnectException e
              (if (= "Operation timed out" (.getMessage e))
                (log/warning (str "Fetching pom from " url " failed because it timed out, retrying"))
                (throw e)))
            (catch java.io.IOException e
              (log/warning (str "Fetching pom from " url " failed because of the following error: " (.getMessage e)))))
          (recur (inc i))))))

(defn ^String get-url
  [^Model model]
  (.getUrl model))

(defn ^Scm get-scm
  [^Model model]
  (.getScm model))

(defn ^String get-scm-url
  [^Scm scm]
  (.getUrl scm))
