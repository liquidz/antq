(ns antq.dep.pom
  "Clojure CLI"
  (:require
   [antq.record :as r]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]))

(def ^:private project-file "pom.xml")

(defn get-value
  [content tag]
  (->> content
       (filter (comp #{tag} :tag))
       first
       :content
       first))

(defn get-values
  [content tags]
  (map #(get-value content %) tags))

(defn extract-deps
  [pom-xml-content-str]
  (->> pom-xml-content-str
       xml/parse-str
       xml-seq
       (filter (comp #{:dependency} :tag))
       (map #(get-values (:content %) [:groupId :artifactId :version]))
       (map (fn [[group-id artifact-id version]]
              (r/map->Dependency {:type :java
                                  :file project-file
                                  :name (str group-id "/" artifact-id)
                                  :version version})))))

(defn load-deps
  []
  (when (.exists (io/file project-file))
    (extract-deps (slurp project-file))))
