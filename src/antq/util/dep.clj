(ns antq.util.dep
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   java.io.File))

(defn compare-deps
  [x y]
  (let [prj (.compareTo ^String (:file x) ^String (:file y))]
    (if (zero? prj)
      (.compareTo ^String (:name x) ^String (:name y))
      prj)))

(defn relative-path
  [^File target-file]
  (-> (.getPath target-file)
      (str/replace-first #"^\./" "")))

(defn name-candidates
  [^String dep-name]
  (let [[group-id artifact-id] (str/split dep-name #"/" 2)
        candidates (cond-> #{}
                     (seq dep-name) (conj (symbol dep-name)))]
    (cond-> candidates
      (= group-id artifact-id) (conj (symbol group-id)))))

(defmulti normalize-by-name
  (fn [dep] (:name dep)))

(defmethod normalize-by-name :default
  [dep]
  dep)

(defn normalize-path
  [^String path]
  (let [file (io/file path)]
    (try
      (let [path' (-> file
                      (.toPath)
                      (.normalize)
                      (str))]
        (if (and (not (str/blank? path))
                 (str/blank? path'))
          "."
          path'))
      (catch Exception _
        (.getCanonicalPath file)))))
