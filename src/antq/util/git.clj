(ns antq.util.git
  (:require
   [antq.log :as log]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(defn- extract-tags
  [ls-remote-resp]
  (some->> (:out ls-remote-resp)
           (str/split-lines)
           (keep #(second (str/split % #"\t" 2)))
           (filter #(= 0 (.indexOf ^String % "refs/tags")))
           (map #(str/replace % #"^refs/tags/" ""))))

(defn- sh-git-ls-remote
  [url]
  (loop [i 0]
    (when (< i 5)
      (let [{:keys [exit err] :as res} (sh/sh "git" "ls-remote" url)]
        (cond
          (= 0 exit)
          res

          (and (< 0 exit) (not (str/includes? err "Operation timed out")))
          (log/error (str "git ls-remote failed on: " url))

          :else
          (do
            (log/error "git ls-remote timed out, retrying")
            (recur (inc i))))))))

(defn tags-by-ls-remote*
  [url]
  (-> (sh-git-ls-remote url)
      (extract-tags)))
(def tags-by-ls-remote
  (memoize tags-by-ls-remote*))
