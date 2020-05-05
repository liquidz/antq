(ns antq.dep.leiningen
  (:require
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "project.clj")

(defn extract-deps
  [project-clj-content-str]
  (let [dep-form? (atom false)
        deps (atom [])]
    (walk/prewalk (fn [form]
                    (cond
                      (keyword? form)
                      (reset! dep-form? (= :dependencies form))

                      (and @dep-form?
                           (vector? form)
                           (vector? (first form)))
                      (swap! deps concat form))
                    form)
                  (read-string (str "(list " project-clj-content-str " )")))
    (for [[dep-name version] @deps]
      (r/map->Dependency {:type :java
                          :project project-file
                          :name  (str dep-name)
                          :version version}))))

(defn load-deps
  []
  (when (.exists (io/file project-file))
    (extract-deps (slurp project-file))))
