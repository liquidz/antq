(ns antq.util.dep
  (:require
   [clojure.string :as str])
  (:import
   java.io.File))

(defn qualified-symbol?'
  "To support Clojure 1.8.0"
  [x]
  (boolean (and (symbol? x) (namespace x) true)))

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
