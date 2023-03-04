(ns antq.dep.babashka
  (:require
   [antq.dep.clojure :as dep.clj]
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.java.io :as io]))

(def ^:private project-file "bb.edn")

(defn load-deps
  {:malli/schema [:function
                  [:=> :cat [:maybe [:sequential r/?dependency]]]
                  [:=> [:cat 'string?] [:maybe [:sequential r/?dependency]]]]}
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (dep.clj/extract-deps (u.dep/relative-path file)
                             (slurp file))))))
