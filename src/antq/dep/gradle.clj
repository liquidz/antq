(ns antq.dep.gradle
  (:require
   [antq.log :as log]
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(def gradle-command "gradle")
(def ^:private project-file "build.gradle")
(def ^:private dep-regexp #"^[^-]\-+\s")

(defn- filter-deps-from-gradle-dependencies
  [file-path]
  (let [{:keys [exit out]} (sh/sh gradle-command
                                  "--build-file" file-path
                                  "--quiet"
                                  "dependencies")]
    (if (= 0 exit)
      (->> (str/split-lines out)
           (filter seq)
           (filter #(re-seq dep-regexp %))
           (map #(str/replace % dep-regexp ""))
           (map #(first (str/split % #" " 2)))
           (set))
      (throw (ex-info "Failed to run gradle" {:exit exit})))))

(defn- convert-grandle-dependency
  "e.g. dep-str: 'org.clojure:clojure:1.10.0'
  NOTE: Extracting repositories is not supported currently"
  [file-path dep-str]
  (let [[group-id artifact-id version] (str/split dep-str #":" 3)]
    (r/map->Dependency {:project :gradle
                        :type :java
                        :file file-path
                        :name (str group-id "/" artifact-id)
                        :version version})))

(defn extract-deps
  [relative-file-path absolute-file-path]
  (try
    (let [deps (filter-deps-from-gradle-dependencies absolute-file-path)
          deps (map #(convert-grandle-dependency relative-file-path %) deps)]
      deps)
    (catch Exception ex
      (log/error (.getMessage ex))
      nil)))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (log/error "WARN: Custom repositories for Gradle are not supported currently.")
       (extract-deps (u.dep/relative-path file)
                     (.getAbsolutePath file))))))
