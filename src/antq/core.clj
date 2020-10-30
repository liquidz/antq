(ns antq.core
  (:gen-class)
  (:require
   [antq.dep.boot :as dep.boot]
   [antq.dep.clojure :as dep.clj]
   [antq.dep.github-action :as dep.gh-action]
   [antq.dep.leiningen :as dep.lein]
   [antq.dep.pom :as dep.pom]
   [antq.dep.shadow :as dep.shadow]
   [antq.record :as r]
   [antq.report :as report]
   [antq.report.edn]
   [antq.report.format]
   [antq.report.json]
   [antq.report.table]
   [antq.ver :as ver]
   [antq.ver.git]
   [antq.ver.github-action]
   [antq.ver.java]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]))

(defn- concat-assoc-fn
  [opt k v]
  (update opt k concat (str/split v #":")))

(def cli-options
  [[nil "--exclude=EXCLUDE" :default [] :assoc-fn concat-assoc-fn]
   [nil "--error-format=ERROR_FORMAT" :default nil]
   [nil "--reporter=REPORTER" :default "table"]
   ["-d" "--directory=DIRECTORY" :default ["."] :assoc-fn concat-assoc-fn]])

(def default-skip-artifacts
  #{"org.clojure/clojure"})

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn skip-artifacts?
  [dep options]
  (let [skip-artifacts (apply conj default-skip-artifacts (:exclude options []))]
    (contains? skip-artifacts (:name dep))))

(defn using-release-version?
  [dep]
  (contains? #{"RELEASE" "master"} (:version dep)))

(defn- assoc-versions
  [dep]
  (assoc dep :_versions (ver/get-sorted-versions dep)))

(defn latest
  [arg-map]
  (let [dep-name (case (:type arg-map)
                   :java (let [[group-id artifact-id] (str/split (str (:name arg-map "")) #"/" 2)]
                           (str group-id "/" (or artifact-id group-id)))
                   (str (:name arg-map)))
        dep-type (:type arg-map :java)]
    (-> (r/map->Dependency
         {:type dep-type
          :name dep-name})
        (ver/get-sorted-versions)
        (first)
        (println))))

(defn- assoc-latest-version
  [dep]
  (let [vers (cond->> (:_versions dep)
               (not (ver/under-devleopment? (:version dep)))
               (drop-while ver/under-devleopment?))
        latest-version (first vers)]
    (assoc dep :latest-version latest-version)))

(defn- dissoc-no-longer-used-keys
  [dep]
  (dissoc dep :_versions))

(defn distinct-deps
  [deps]
  (->> deps
       (map #(select-keys % [:type :name :version :repositories :extra]))
       (map #(if (ver/snapshot? (:version %))
               %
               (dissoc % :version)))
       distinct))

(defn complete-versions-by
  [dep deps-with-vers]
  (if-let [dep-with-vers (some #(and (= (:type dep) (:type %))
                                     (= (:name dep) (:name %))
                                     %)
                               deps-with-vers)]
    (assoc dep :_versions (:_versions dep-with-vers))
    dep))

(defn outdated-deps
  [deps options]
  (let [org-deps (remove #(or (skip-artifacts? % options)
                              (using-release-version? %))
                         deps)
        uniq-deps-with-vers (->> org-deps
                                 distinct-deps
                                 (pmap assoc-versions))]
    (->> org-deps
         (pmap #(complete-versions-by % uniq-deps-with-vers))
         (map (comp dissoc-no-longer-used-keys
                    assoc-latest-version))
         (remove ver/latest?))))

(defn exit
  [outdated-deps]
  (System/exit (if (seq outdated-deps) 1 0)))

(defn fetch-deps
  [options]
  (mapcat #(concat (dep.boot/load-deps %)
                   (dep.clj/load-deps %)
                   (dep.gh-action/load-deps %)
                   (dep.pom/load-deps %)
                   (dep.shadow/load-deps %)
                   (dep.lein/load-deps %))
          (distinct (:directory options))))

(defn -main
  [& args]
  (let [{:keys [options]} (cli/parse-opts args cli-options)
        options (cond-> options
                  ;; Force "format" reporter when :error-format is specified
                  (some?  (:error-format options)) (assoc :reporter "format"))
        deps (fetch-deps options)]
    (if (seq deps)
      (let [outdated (outdated-deps deps options)]
        (report/reporter outdated options)
        (exit outdated))
      (do (println "No project file")
          (System/exit 1)))))
