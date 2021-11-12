(ns antq.util.leiningen
  (:require
   [clojure.string :as str]))

(defn- getenv
  [x]
  (System/getenv x))

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
          (getenv)))
