(ns antq.date.git-tag-and-sha
  (:require
   [antq.date :as date]
   [antq.util.git :as u.git]))

(defmethod date/get-last-updated-at :git-tag-and-sha
  [dep _options]
  (let [url (get-in dep [:extra :url])
        lib (some-> dep :name symbol)]
    (when (and url lib)
      (u.git/head-date url lib))))
