(ns antq.dep.babashka
  (:require
   [antq.dep.clojure :as dep.clj]
   [antq.util.dep :as u.dep]
   [clojure.java.io :as io]))

(def ^:private project-file "bb.edn")

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (dep.clj/extract-deps (u.dep/relative-path file)
                             (slurp file))))))
