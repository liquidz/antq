(ns antq.ver.circle-ci-orb
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [antq.ver :as ver]))

(defn orb-id [ns name]
  (-> (io/as-url (str "https://internal.circleci.com/api/v2/orbs?ns=" ns "&name=" name))
      slurp
      (json/read-str :key-fn keyword)
      :items
      first
      :id))

(defn orb-versions [id]
  (-> (io/as-url (str "https://internal.circleci.com/api/v2/orbs/" id))
      slurp
      (json/read-str :key-fn keyword)
      :versions))

(defmethod ver/get-sorted-versions :circle-ci-orb
  [dep _options]
  (let [[ns name] (str/split (:name dep) #"/")
        id (orb-id ns name)]
    (orb-versions id)))
