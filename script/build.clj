(ns script.build
  (:require
   [clojure.tools.build.api :as b]
   [org.corfield.build :as bb]))

(defn ^:private the-version
  [patch]
  (format "1.4.%s" patch))

(def ^:private revs (Integer/parseInt (b/git-count-revs nil)))
(def ^:private version (the-version revs))
(def ^:private snapshot (the-version (format "%s-SNAPSHOT" (inc revs))))
(def ^:private library 'com.github.liquidz/antq)

(defn jar
  "JAR the artifact.

   This task will create the JAR in the `target` directory.
   "
  [{:keys [tag] :or {tag snapshot} :as opts}]
  (-> opts
      (assoc :lib library :version tag :tag tag)
      (bb/clean)
      (bb/jar)))

(def ^:private config {:main 'antq.core
                       :uber-file "target/antq.jar"})

(defn uberjar
  "UberJAR the application.

   This task will create the UberJAR in the `target` directory.
   "
  [opts]
  (-> (merge config opts)
      (bb/clean)
      (bb/uber)))

(defn deploy
  "Deploy the JAR to your local repository (proxy).

   This task will build and deploy the JAR to your
   local repository using `deps-deploy`. This requires
   the following environment variables being set beforehand:

   CLOJARS_URL, CLOJARS_USERNAME, CLOJARS_PASSWORD

   Even although they are CLOJARS environment variables, they
   can actually point to anywhere, like your own Nexus OSS repository
   or an Artifactory repository for example.

   You may want to consider something like `direnv` to manage your
   per-directory loading of environment variables.
   "
  [{:keys [tag] :or {tag snapshot} :as opts}]
  (-> opts
      (assoc :lib library :version tag :tag tag)
      (bb/deploy)))

(defn install
  "Deploy the JAR to your local .m2 directory"
  [{:keys [tag] :or {tag snapshot} :as opts}]
  (-> opts
      (assoc :lib library
             :version tag
             :tag tag)
      (bb/install)))
