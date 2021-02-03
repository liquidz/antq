(ns antq.diff.git-sha
  (:require
   [antq.diff :as diff]
   [antq.util.url :as u.url]
   [clojure.string :as str]))

(defmethod diff/get-diff-url :git-sha
  [dep]
  (when-let [url (get-in dep [:extra :url])]
    (cond
      (str/starts-with? url "https://github.com/")
      (format "%scompare/%s...%s"
              (u.url/ensure-tail-slash url)
              (:version dep)
              (:latest-version dep))

      :else
      (println "Diff is not supported for" url))))
