(ns antq.dep.pom
  "Clojure CLI"
  (:require
   [antq.record :as r]
   [antq.util.xml :as u.xml]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]))

(def ^:private project-file "pom.xml")

(defn extract-repos
  [xml]
  (->> xml
       (u.xml/get-tags :repository)
       (map #(u.xml/get-values [:id :url] (:content %)))
       (reduce #(assoc %1 (first %2) {:url (second %2)}) {})))

(defn extract-deps
  [pom-xml-content-str]
  (let [xml (->> pom-xml-content-str
                 xml/parse-str
                 xml-seq)
        repos (extract-repos xml)]
    (->> (u.xml/get-tags :dependency xml)
         (map #(u.xml/get-values [:groupId :artifactId :version] (:content %)))
         (map (fn [[group-id artifact-id version]]
                (r/map->Dependency {:type :java
                                    :file project-file
                                    :name (str group-id "/" artifact-id)
                                    :version version
                                    :repositories repos}))))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (slurp file))))))
