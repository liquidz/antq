(ns antq.dep.clojure
  "Clojure CLI"
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "deps.edn")

(defn- ignore?
  [opt]
  (contains? opt :local/root))

(defmulti extract-type-and-version
  (fn [opt]
    (or (and (:mvn/version opt) :java)
        (and (:git/url opt) :git))))

(defmethod extract-type-and-version :default
  [opt]
  {:type :java
   :version (:mvn/version opt)})

(defmethod extract-type-and-version :git
  [opt]
  {:type :git-sha
   :version (:sha opt)
   :extra {:url (:git/url opt)}})

(defn extract-deps
  [file-path deps-edn-content-str]
  (let [deps (atom {})
        edn (edn/read-string deps-edn-content-str)]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (#{:deps :extra-deps} (first form)))
                       (swap! deps merge (second form)))
                     form)
                   edn)
    (for [[dep-name opt] @deps
          :let [type-and-version (extract-type-and-version opt)]
          :when (and (not (ignore? opt))
                     (string? (:version type-and-version))
                     (seq (:version type-and-version)))]
      (-> {:project :clojure
           :file file-path
           :name  (if (u.dep/qualified-symbol?' dep-name)
                    (str dep-name)
                    (str dep-name "/" dep-name))
           :repositories (:mvn/repos edn)}
          (merge type-and-version)
          (r/map->Dependency)))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
