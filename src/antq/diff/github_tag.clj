(ns antq.diff.github-tag
  (:require
   [antq.diff :as diff]
   [antq.util.git :as u.git]
   [clojure.string :as str]))

(defn- exact-or-included
  [coll ^String target]
  (or (some #(and (= target %) %) coll)
      (first (filter #(str/includes? % target) coll))))

(defmethod diff/get-diff-url :github-tag
  [dep]
  (let [url (format "https://github.com/%s"
                    (str/join "/" (take 2 (str/split (:name dep) #"/"))))
        tags (u.git/tags-by-ls-remote url)
        current (or (exact-or-included tags (:version dep))
                    (:version dep))
        latest (or (exact-or-included tags (:latest-version dep))
                   (:latest-version dep))]
    (format "%s/compare/%s...%s" url current latest)))
