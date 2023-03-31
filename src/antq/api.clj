(ns antq.api
  (:require
   [antq.core :as core]
   [antq.dep.clojure :as dep.clojure]
   [antq.report :as report]))

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
  ([deps-map {:as options :keys [repositories]}]
   (let [deps-edn (cond-> {:deps deps-map}
                    repositories (assoc :mvn/repos repositories))
         antq-deps (dep.clojure/extract-deps "" (pr-str deps-edn))
         antq-options (-> options
                          (dissoc :repositories)
                          (assoc :reporter report/no-output-reporter))]
     (core/antq antq-options antq-deps))))

(comment
  (outdated-deps '{org.clojure/clojure {:mvn/version "1.8.0"}}
                 {:no-changes true}))
