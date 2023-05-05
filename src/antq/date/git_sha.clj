(ns antq.date.git-sha
  (:require
   [antq.date :as date]
   [antq.util.git :as u.git]))

(defmethod date/get-last-updated-at :git-sha
  [dep _options]
  (let [url (get-in dep [:extra :url])
        lib (some-> dep :name symbol)]
    (when (and url lib)
      (u.git/head-date url lib))))
