(ns antq.dep.leiningen
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "project.clj")

(defn normalize-repositories
  [repos]
  (reduce (fn [acc [k v]] (assoc acc k (if (map? v) v {:url v}))) {} repos))

(defn normalize-name
  [dep-name]
  (if (u.dep/qualified-symbol?' dep-name)
    (str dep-name)
    (str dep-name "/" dep-name)))

(defn acceptable-version?
  [version]
  (and (string? version) (seq version)))

(defn extract-deps
  [file-path project-clj-content-str]
  (let [dep-form? (atom false)
        repos-form? (atom false)
        deps (atom [])
        repos (atom [])]
    (walk/prewalk (fn [form]
                    (cond
                      (keyword? form)
                      (do (reset! dep-form? (#{:dependencies :plugins} form))
                          (reset! repos-form? (= :repositories form)))

                      (and @dep-form?
                           (vector? form)
                           (vector? (first form)))
                      (swap! deps concat form)

                      (and @repos-form?
                           (vector? form)
                           (vector? (first form)))
                      (swap! repos concat form))
                    form)
                  (read-string (str "(list " project-clj-content-str " )")))
    (let [repositories (normalize-repositories @repos)]
      (for [[dep-name version] @deps
            :when (acceptable-version? version)]
        (r/map->Dependency {:project :leiningen
                            :type :java
                            :file file-path
                            :name (normalize-name dep-name)
                            :version version
                            :repositories repositories})))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
