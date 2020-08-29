(ns antq.ver.git
  (:require
   [antq.ver :as ver]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(defn extract-head-sha
  [ls-remote-resp]
  (->> (:out ls-remote-resp)
       (str/split-lines)
       (some (fn [line]
               (let [[sha ref-name] (str/split line #"\t" 2)]
                 (and (= "HEAD" ref-name)
                      sha))))))

(defmethod ver/get-sorted-versions :git
  [dep]
  (let [resp (->> (get-in dep [:extra :url])
                  (sh/sh "git" "ls-remote"))]
    (or (some-> (extract-head-sha resp) vector)
        [])))

(defmethod ver/latest? :git
  [dep]
  (let [current (some-> dep :version)
        latest (some-> dep :latest-version)]
    (= latest current)))
