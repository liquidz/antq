(ns antq.util.file
  (:require
   [antq.util.env :as u.env]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn- normalize-home
  [file-path]
  (if-let [home (u.env/getenv "HOME")]
    (str/replace file-path home "~")
    file-path))

(defn- normalize-path*
  [file-path]
  (-> (io/file file-path)
      (.toPath)
      (.normalize)
      (str)))

(def ^{:malli/schema [:=> [:cat 'string?] 'string?]}
  normalize-path
  (comp normalize-home
        normalize-path*))
