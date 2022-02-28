(ns antq.dep.clojure.tool
  (:require
   [antq.record :as r]
   [antq.util.env :as u.env]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn extract-deps
  [file-path tool-edn-content-str]
  (let [edn (edn/read-string tool-edn-content-str)
        {:git/keys [sha tag url]} (:coord edn)]
    (when (and sha tag url)
      (r/map->Dependency {:type :git-tag-and-sha
                          :file file-path
                          :name (str (:lib edn))
                          :project :clojure-tool
                          :version tag
                          :extra {:url url :sha sha}}))))

(defn load-deps
  ([]
   (load-deps (io/file (u.env/getenv "HOME") ".clojure" "tools")))
  ([dir-file]
   (some->> dir-file
            (file-seq)
            (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn")))
            (keep #(extract-deps (.getAbsolutePath %) (slurp %))))))
