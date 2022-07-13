(ns antq.util.file
  (:require
   [antq.util.env :as u.env]
   [clojure.string :as str]))

(defn normalize-path
  [file-path]
  (if-let [home (u.env/getenv "HOME")]
    (str/replace file-path home "~")
    file-path))
