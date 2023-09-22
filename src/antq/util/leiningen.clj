(ns antq.util.leiningen
  (:require
   [antq.util.env :as u.env]
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]))

(defn lein-home
  []
  (io/file (System/getProperty "user.home") ".lein"))

(defn- env-name
  "cf. https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#credentials-in-the-environment"
  [kw]
  (cond
    (and (qualified-keyword? kw)
         (= "env" (namespace kw)))
    (str/upper-case (name kw))

    (= :env kw)
    "LEIN_PASSWORD"

    :else
    nil))

(defn- gpg-program
  []
  (or (System/getenv "LEIN_GPG") "gpg"))

(defn- gpg
  [& args]
  (try
    (apply shell/sh (gpg-program) args)
    (catch java.io.IOException e
      {:exit 1 :err (.getMessage e)})))

(defn- credentials-fn
  "cf. https://leiningen.org/reference.html"
  ([] (let [cred-file (io/file (lein-home) "credentials.clj.gpg")]
        (if (.exists cred-file)
          (credentials-fn cred-file))))
  ([file]
   (let [{:keys [out err exit]} (gpg "--quiet" "--batch"
                                     "--decrypt" "--" (str file))]
     (if (pos? exit)
       (binding [*out* *err*]
         (println "Could not decrypt credentials from" (str file))
         (println err)
         (println "See `lein help gpg` for how to install gpg."))
       (read-string out)))))

(defn read-credentials
  []
  (credentials-fn))

(defn env
  [kw]
  (some-> (env-name kw)
          (u.env/getenv)))
