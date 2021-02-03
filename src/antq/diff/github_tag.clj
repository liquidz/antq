(ns antq.diff.github-tag
  (:require
   [antq.diff :as diff]
   [clojure.string :as str]))

(defmethod diff/get-diff-url :github-tag
  [dep]
  (format "https://github.com/%s/compare/%s...%s"
          (str/join "/" (take 2 (str/split (:name dep) #"/")))
          (:version dep)
          (:latest-version dep)))
