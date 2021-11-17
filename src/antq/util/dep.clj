(ns antq.util.dep
  (:require
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
  (let [sep (System/getProperty "file.separator")]
    (loop [[v :as elements] (seq (.split path sep))
           accm []]
      (if-not v
        (str/join sep accm)
        (recur (rest elements)
               (condp = v
                 "." (cond
                       (seq accm) accm
                       (seq (rest elements))  accm
                       :else (conj accm v))

                 ".." (if (seq accm)
                        (vec (butlast accm))
                        (conj accm v))
                 (conj accm v)))))))
