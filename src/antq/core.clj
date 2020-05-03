(ns antq.core
  (:gen-class)
  (:require
   [ancient-clj.core :as ancient]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.walk :as walk]
   [version-clj.core :as version]))

(defrecord Dependency [type name version latest-version])

(def default-skip-artifacts
  #{"org.clojure/clojure"})

(def default-repos
  {"central" "https://repo1.maven.org/maven2/"
   "clojars" "https://repo.clojars.org/"})

(defn extract-deps
  [deps-edn-content]
  (let [deps (atom {})]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (#{:deps :extra-deps} (first form)))
                       (swap! deps merge (second form)))
                     form)
                   deps-edn-content)
    (for [[dep-name {:mvn/keys [version]}] @deps]
      (map->Dependency {:type :java :name  (str dep-name) :version version}))))

(defn skip-artifacts? [^Dependency dep]
  (contains? default-skip-artifacts (:name dep)))

(defn using-release-version? [^Dependency dep]
  (contains? #{"RELEASE"} (:version dep)))

(defn get-latest-version
  [^Dependency dep]
  (ancient/latest-version-string!
   (:name dep)
   {:repositories default-repos
    :snapshots? false}))

(defn latest?
  [^Dependency dep]
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
       (map #(assoc % :latest-version (get-latest-version %)))
       (remove latest?)))

(defn print-deps
  [deps]
  (if (seq deps)
    (->> deps
         (map #(update % :latest-version (fnil identity "Failed to fetch")))
         (pprint/print-table [:name :version :latest-version]))
    (println "All dependencies are up-to-date."))
  deps)

(defn exit
  [outdated-deps]
  (System/exit (if (seq outdated-deps) 1 0)))

(defn -main
  []
  (cond
    (.exists (io/file "deps.edn"))
    (-> (slurp "deps.edn")
        edn/read-string
        extract-deps
        outdated-deps
        print-deps
        exit)

    :else
    (do (println "No project file")
        (System/exit 1))))
