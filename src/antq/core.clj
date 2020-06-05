(ns antq.core
  (:gen-class)
  (:require
   [antq.dep.boot :as dep.boot]
   [antq.dep.clojure :as dep.clj]
   [antq.dep.github-action :as dep.gh-action]
   [antq.dep.leiningen :as dep.lein]
   [antq.dep.pom :as dep.pom]
   [antq.dep.shadow :as dep.shadow]
   [antq.format :as fmt]
   [antq.ver :as ver]
   [antq.ver.github-action]
   [antq.ver.java]
   [clojure.tools.cli :as cli]))

(def cli-options
  [[nil "--exclude=EXCLUDE" :default [] :assoc-fn #(update %1 %2 conj %3)]])

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

(defn outdated-deps
  [deps options]
  (->> deps
       (remove #(or (skip-artifacts? % options)
                    (using-release-version? %)))
       (pmap assoc-versions)
       (map (comp dissoc-no-longer-used-keys
                  assoc-latest-version))
       (remove ver/latest?)))



(defn exit
  [outdated-deps]
  (System/exit (if (seq outdated-deps) 1 0)))

(defn fetch-deps
  []
  (concat (dep.boot/load-deps)
          (dep.clj/load-deps)
          (dep.gh-action/load-deps)
          (dep.pom/load-deps)
          (dep.shadow/load-deps)
          (dep.lein/load-deps)))

(defn -main
  [& args]
  (let [{:keys [options]} (cli/parse-opts args cli-options)
        deps (fetch-deps)]
    (if (seq deps)
      (-> deps
          (outdated-deps options)
          (fmt/print-deps options)
          exit)
      (do (println "No project file")
          (System/exit 1)))))
