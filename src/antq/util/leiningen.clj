(ns antq.util.leiningen
  (:require
   [antq.util.env :as u.env]
   [clojure.string :as str]))

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

(defn env
  [kw]
  (some-> (env-name kw)
          (u.env/getenv)))
