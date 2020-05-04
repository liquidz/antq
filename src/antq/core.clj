(ns antq.core
  (:gen-class)
  (:require
   [ancient-clj.core :as ancient]
   [antq.dep.clojure :as dep.clj]
   [antq.dep.leiningen :as dep.lein]
   [antq.dep.pom :as dep.pom]
   [antq.dep.shadow :as dep.shadow]
   [clojure.pprint :as pprint]
   [version-clj.core :as version]))

(def default-skip-artifacts
  #{"org.clojure/clojure"})

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn skip-artifacts?
  [dep]
  (contains? default-skip-artifacts (:name dep)))

(defn using-release-version?
  [dep]
  (contains? #{"RELEASE"} (:version dep)))

(defn get-latest-version
  [dep]
  (ancient/latest-version-string!
   (:name dep)
   {:repositories default-repos
    :snapshots? false}))

(defn latest?
  [dep]
  (and (:version dep)
       (:latest-version dep)
       (zero?  (version/version-compare
                (:version dep)
                (:latest-version dep)))))

(defn outdated-deps
  [deps]
  (->> deps
       (remove skip-artifacts?)
       (remove using-release-version?)
       (pmap #(assoc % :latest-version (get-latest-version %)))
       (remove latest?)))

(defn print-deps
  [deps]
  (if (seq deps)
    (let [grp (group-by :project deps)
          project-names (sort (keys grp))]
      (doseq [project-name project-names]

        (println "\n###" project-name)
        (->> (get grp project-name)
             (map #(update % :latest-version (fnil identity "Failed to fetch")))
             (pprint/print-table [:name :version :latest-version]))))
    (println "All dependencies are up-to-date."))
  deps)

(defn exit
  [outdated-deps]
  (System/exit (if (seq outdated-deps) 1 0)))

(defn -main
  []
  (let [deps (concat (dep.lein/load-deps)
                     (dep.pom/load-deps)
                     (dep.shadow/load-deps)
                     (dep.clj/load-deps))]
    (if (seq deps)
      (-> deps
          outdated-deps
          print-deps
          exit)
      (do (println "No project file")
          (System/exit 1)))))
