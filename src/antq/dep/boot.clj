(ns antq.dep.boot
  (:require
   [antq.record :as r]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "build.boot")

(defn extract-deps
  [build-boot-content-str]
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
                  (read-string (str "(list " build-boot-content-str " )")))
    (for [[dep-name version] @deps]
      (r/map->Dependency {:type :java
                          :file project-file
                          :name  (str dep-name)
                          :version version}))))

(defn load-deps
  []
  (when (.exists (io/file project-file))
    (extract-deps (slurp project-file))))
