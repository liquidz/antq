(ns antq.dep.leiningen
  (:require
   [antq.constant :as const]
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
  (if (qualified-symbol? dep-name)
    (str dep-name)
    (str dep-name "/" dep-name)))

(defn acceptable-version?
  [version]
  (and (string? version) (seq version)))

(defn- exclude?
  [v]
  (-> (meta v)
      (contains? const/deps-exclude-key)))

(defn extract-deps
  {:malli/schema [:=>
                  [:cat 'string? 'string?]
                  [r/?dependencies]]}
  [file-path project-clj-content-str]
  (let [dep-form? (atom false)
        repos-form? (atom false)
        deps (atom [])
        repos (atom [])]
    (walk/prewalk (fn [form]
                    (cond
                      (keyword? form)
                      (do (reset! dep-form? (#{:dependencies :managed-dependencies :plugins} form))
                          (reset! repos-form? (= :repositories form)))

                      (and @dep-form?
                           (vector? form)
                           (vector? (first form)))
                      (->> form
                           (seq)
                           (remove exclude?)
                           (swap! deps concat))

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
  {:malli/schema [:function
                  [:=> :cat [:maybe [r/?dependencies]]]
                  [:=> [:cat 'string?] [:maybe r/?dependencies]]]}
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
