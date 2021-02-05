(ns antq.util.git
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(defn- extract-tags
  [ls-remote-resp]
  (->> (:out ls-remote-resp)
       (str/split-lines)
       (keep #(second (str/split % #"\t" 2)))
       (filter #(= 0 (.indexOf ^String % "refs/tags")))
       (map #(str/replace % #"^refs/tags/" ""))))

(defn tags-by-ls-remote*
  [url]
  (-> (sh/sh "git" "ls-remote" url)
      (extract-tags)))
(def tags-by-ls-remote
  (memoize tags-by-ls-remote*))
