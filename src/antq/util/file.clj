(ns antq.util.file
  (:require
   [antq.util.env :as u.env]
   [clojure.string :as str]))

(defn normalize-path
  [file-path]
  (str/replace file-path (u.env/getenv "HOME") "~"))
