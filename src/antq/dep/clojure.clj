(ns antq.dep.clojure
  "Clojure CLI"
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.deps.alpha.extensions.git :as git]
   [clojure.walk :as walk]))

(def ^:private project-file "deps.edn")

(defn- ignore?
  [opt]
  (and (map? opt)
       (contains? opt :local/root)))

(defmulti extract-type-and-version
  (fn [opt]
    (if (map? opt)
      (cond
        (contains? opt :mvn/version)
        :java

        (and (contains? opt :git/url)
             (some #(contains? opt %) [:tag :git/tag])
             (some #(contains? opt %) [:sha :git/sha]))
        :git-tag-and-sha

        (and (contains? opt :git/url)
             (some #(contains? opt %) [:sha :git/sha]))
        :git-sha

        :else
        ::unknown)
      ::unknown)))

(defmethod extract-type-and-version :default
  [_]
  {})

(defmethod extract-type-and-version :java
  [opt]
  {:type :java
   :version (:mvn/version opt)})

(defmethod extract-type-and-version :git-sha
  [opt]
  {:type :git-sha
   :version (:sha opt (:git/sha opt))
   :extra {:url (:git/url opt)}})

(defmethod extract-type-and-version :git-tag-and-sha
  [opt]
  {:type :git-tag-and-sha
   :version (:tag opt (:git/tag opt))
   :extra {:url (:git/url opt)
           :sha (:sha opt (:git/sha opt))}})

(defn- adjust-version-via-deduction
  [dep-name opt]
  (if (and (map? opt)
           (some #{:tag :sha :git/tag :git/sha} (keys opt))
           (not (:git/url opt)))
    (if-let [git-dep (git/auto-git-url dep-name)]
      (assoc opt :git/url git-dep)
      opt)
    opt))

(defn extract-deps
  [file-path deps-edn-content-str]
  (let [deps (atom [])
        edn (edn/read-string deps-edn-content-str)]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (#{:deps :extra-deps :replace-deps :override-deps} (first form))
                                (map? (second form)))
                       (->> form
                            (second)
                            (seq)
                            (swap! deps concat)))
                     form)
                   edn)
    (for [[dep-name opt] @deps
          :let [opt (adjust-version-via-deduction dep-name opt)
                type-and-version (extract-type-and-version opt)]
          :when (and (not (ignore? opt))
                     (string? (:version type-and-version))
                     (seq (:version type-and-version)))]
      (-> {:project :clojure
           :file file-path
           :name  (if (qualified-symbol? dep-name)
                    (str dep-name)
                    (str dep-name "/" dep-name))
           :repositories (:mvn/repos edn)}
          (merge type-and-version)
          (r/map->Dependency)))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
