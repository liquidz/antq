(ns antq.ver.git-sha
  (:require
   [antq.ver :as ver]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(defn- extract-head-sha
  [ls-remote-resp]
  (some->> (:out ls-remote-resp)
           (str/split-lines)
           (some (fn [line]
                   (let [[sha ref-name] (str/split line #"\t" 2)]
                     (and (= "HEAD" ref-name)
                          sha))))))

(defn- git-ls-remote
  [url]
  (sh/sh "git" "ls-remote" url))

(defmethod ver/get-sorted-versions :git-sha
  [dep]
  (let [resp (->> (get-in dep [:extra :url])
                  (git-ls-remote))]
    (or (some-> (extract-head-sha resp) vector)
        [])))

(defmethod ver/latest? :git-sha
  [dep]
  (let [current (some-> dep :version)
        latest (some-> dep :latest-version)]
    (= latest current)))
