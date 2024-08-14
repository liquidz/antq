(ns antq.api
  (:require
   [antq.core :as core]
   [antq.dep.clojure :as dep.clojure]
   [antq.report :as report]
   [antq.upgrade :as upgrade]
   [antq.util.file :as u.file]))

(defn outdated-deps
  "Returns outdated dependencies in the form of `antq.record.Dependency`.

  - deps-map (Required)
    A map of the same form as `:deps` in deps.edn.
    E.g. '{org.clojure/clojure {:mvn/version \"1.11.1\"}}
  - options (Optional)
    A CLI options map including additional API options.

    API options:
    - repositories
      A map of the same form as `:mvn/repos` in deps.edn.
      E.g. {\"clojars\" {:url \"https://clojars.org/repo\"}}"
  ([deps-map]
   (outdated-deps deps-map {}))
  ([deps-map {:as options :keys [repositories file-path] :or {file-path ""}}]
   (let [deps-edn (cond-> {:deps deps-map}
                    repositories (assoc :mvn/repos repositories))
         antq-deps (dep.clojure/extract-deps file-path (pr-str deps-edn))
         antq-options (-> options
                          (dissoc :repositories)
                          (assoc :reporter report/no-output-reporter))]
     (core/antq antq-options antq-deps))))

(defn upgrade-deps!
  "Upgrade version strings in specified files.
  Returns a map as follows.
  {true [upgraded-deps] false [non-upgraded deps]}

  - file-dep-pairs (Required)
    A vector of maps as follows.
    {:file \"File path to upgrade\"
     :dependency outdated-dependency-map}
  - options (Optional)
    A CLI options map."
  ([file-dep-pairs]
   (upgrade-deps! file-dep-pairs {}))
  ([file-dep-pairs options]
   (let [deps (map #(assoc (:dependency %)
                           :file (:file %)
                           :project (u.file/detect-project (:file %)))
                   file-dep-pairs)
         options (assoc options
                        :force true
                        :reporter report/no-output-reporter)]
     (upgrade/upgrade! deps options))))

(comment
  (outdated-deps '{org.clojure/clojure {:mvn/version "1.8.0"}}
                 {:no-changes true})

  (upgrade-deps! [{:file "/tmp/deps.edn"
                   :dependency {:project :clojure
                                :type :java
                                :name "org.clojure/clojure"
                                :version "1.8.0"
                                :latest-version "1.11.1"}}]))
