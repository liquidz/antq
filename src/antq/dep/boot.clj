(ns antq.dep.boot
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "build.boot")

(defn extract-deps
  [file-path build-boot-content-str]
  (let [dep-form? (atom false)
        repos-form? (atom false)
        deps (atom [])
        repos (atom [])]
    (walk/prewalk (fn [form]
                    (cond
                      (keyword? form)
                      (do (reset! dep-form? (= :dependencies form))
                          (reset! repos-form? (= :repositories form)))

                      (and @dep-form?
                           (vector? form)
                           (vector? (first form)))
                      (swap! deps concat form)

                      (and @repos-form?
                           (vector? form)
                           (string? (first form)))
                      (swap! repos concat form))
                    form)
                  (read-string (str "(list " build-boot-content-str " )")))
    (let [repositories (apply hash-map @repos)]
      (for [[dep-name version] @deps
            :when (and (string? version) (seq version))]
        (r/map->Dependency {:project :boot
                            :type :java
                            :file file-path
                            :name  (if (u.dep/qualified-symbol?' dep-name)
                                     (str dep-name)
                                     (str dep-name "/" dep-name))
                            :version version
                            :repositories repositories})))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
