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
  [{:mvn/keys [version]}]
  {:type :java
   :version version})

(defmethod extract-type-and-version :git
  [{:git/keys [url] :keys [sha]}]
  {:type :git
   :version sha
   :extra {:url url}})

(defn extract-deps
  [file-path deps-edn-content-str]
  (let [deps (atom {})
        {:mvn/keys [repos] :as edn} (edn/read-string deps-edn-content-str)]
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
      (-> {:file file-path
           :name  (if (qualified-symbol? dep-name)
                    (str dep-name)
                    (str dep-name "/" dep-name))
           :repositories repos}
          (merge type-and-version)
          (r/map->Dependency)))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
