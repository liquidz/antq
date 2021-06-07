(ns antq.dep.gradle
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(def ^:private project-file "build.gradle")
(def ^:private dep-regexp #"^[^-]\-+\s")

(defn- filter-deps-from-gradle-dependencies
  [file-path]
  (let [{:keys [exit out]} (sh/sh "gradle" "--build-file" file-path "--quiet" "dependencies")]
    (if (= 0 exit)
      (->> (str/split-lines out)
           (filter seq)
           (filter #(re-seq dep-regexp %))
           (map #(str/replace % dep-regexp ""))
           (map #(first (str/split % #" " 2)))
           (set))
      (throw (ex-info "Failed to run gradle" {:exit exit})))))

(defn- convert-grandle-dependency
  "e.g. dep-str: 'org.clojure:clojure:1.10.0'"
  [file-path dep-str]
  (let [[group-id artifact-id version] (str/split dep-str #":" 3)]
    (r/map->Dependency {:project :gradle
                        :type :java
                        :file file-path
                        :name (str group-id "/" artifact-id)
                        :version version
                        #_#_:repositories repositories})))

(defn extract-deps
  [file-path]
  (try
    (let [deps (filter-deps-from-gradle-dependencies file-path)
          deps (map #(convert-grandle-dependency file-path %) deps)]
      deps)
    (catch Exception _
      ;; FIXME
      nil)))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file))))))
