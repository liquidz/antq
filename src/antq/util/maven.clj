(ns antq.util.maven
  (:require
   [antq.constant :as const]
   [antq.log :as log]
   [antq.util.leiningen :as u.lein]
   [antq.util.xml :as u.xml]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps.alpha.util.maven :as deps.util.maven]
   [clojure.tools.deps.alpha.util.session :as deps.util.session])
  (:import
   (java.net
    Authenticator
    PasswordAuthentication)
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
    (transferStarted [_ _event])
    (transferCorrupted [_ event]
      (log/warning (str "Download corrupted:" (.. ^TransferEvent event getException getMessage))))
    ;; This happens when Maven can't find an artifact in a particular repo
    ;; (but still may find it in a different repo), ie this is a common event
    (transferFailed [_ _event])
    (transferInitiated [_ _event])
    (transferProgressed [_ _event])
    (transferSucceeded [_ _event])))

(defn repository-system
  [name version opts]
  (let [lib (cond-> name (string? name) symbol)
        local-repo @deps.util.maven/cached-local-repo
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
  (when-not (str/includes? url "s3://") ; can't do diff's on s3:// repos, https://github.com/liquidz/antq/issues/133.
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
            (recur (inc i)))))))

(defn ^String get-url
  [^Model model]
  (.getUrl model))

(defn ^Scm get-scm
  [^Model model]
  (.getScm model))

(defn ^String get-scm-url
  [^Scm scm]
  (.getUrl scm))

(defn- get-local-versions*
  [name]
  (let [sep (System/getProperty "file.separator")
        path (-> (str name)
                 (str/replace "/" sep)
                 (str/replace "." sep))
        file (io/file (System/getenv "HOME") ".m2" "repository" path "maven-metadata-local.xml")]
    (when (.exists file)
      (try
        (->> (slurp file)
             (xml/parse-str)
             (xml-seq)
             (u.xml/get-tags :version)
             (map (comp first :content)))
        (catch Exception ex
          (log/warning (str "Failed to get local versions for "
                            name
                            " from "
                            (.getAbsolutePath file)
                            " because: "
                            (.getMessage ex)))
          nil)))))

(def get-local-versions
  (memoize get-local-versions*))

(defn ^Authenticator authenticator
  [^String username ^String password]
  (proxy [Authenticator] []
    (getPasswordAuthentication []
      (PasswordAuthentication. username (char-array password)))))

(defn initialize-proxy-setting!
  []
  (when-let [prxy (some-> (get-maven-settings {})
                          (.getActiveProxy))]
    (let [host (.getHost prxy)
          port (.getPort prxy)
          username (.getUsername prxy)
          password (.getPassword prxy)]
      (System/setProperty "http.proxyHost" host)
      (System/setProperty "http.proxyPort" (str port))
      (System/setProperty "https.proxyHost" host)
      (System/setProperty "https.proxyPort" (str port))
      (when (and username password)
        (System/setProperty "jdk.http.auth.tunneling.disabledSchemes" "")
        (System/setProperty "jdk.http.auth.proxying.disabledSchemes" "")
        (Authenticator/setDefault (authenticator username password))))))
