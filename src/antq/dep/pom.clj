(ns antq.dep.pom
  "Clojure CLI"
  (:require
   [antq.record :as r]
   [antq.util.xml :as u.xml]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]))

(def ^:private project-file "pom.xml")

(defn extract-deps
  [pom-xml-content-str]
  (->> pom-xml-content-str
       xml/parse-str
       xml-seq
       (filter (comp #{:dependency} :tag))
       (map #(u.xml/get-values (:content %) [:groupId :artifactId :version]))
       (map (fn [[group-id artifact-id version]]
              (r/map->Dependency {:type :java
                                  :file project-file
                                  :name (str group-id "/" artifact-id)
                                  :version version})))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (slurp file))))))
