(ns antq.dep.shadow
  (:require
   [antq.record :as r]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "shadow-cljs.edn")

(defn extract-deps
  [shadow-cljs-edn-content-str]
  (let [deps (atom [])]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (= :dependencies (first form)))
                       (swap! deps concat (second form)))
                     form)
                   (edn/read-string shadow-cljs-edn-content-str))
    (for [[dep-name version] @deps]
      (r/map->Dependency {:type :java
                          :file project-file
                          :name  (str dep-name)
                          :version version}))))

(defn load-deps
  []
  (when (.exists (io/file project-file))
    (extract-deps (slurp project-file))))
