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
  - repositories (Optional)
    A map of the same form as `:mvn/repos` in deps.edn.
    E.g. {\"clojars\" {:url \"https://clojars.org/repo\"}}
  - no-changes? (Optional)
    If true, skip checking changes between deps' versions.
    Default is false."
  ([deps-map]
   (outdated-deps deps-map {}))
  ([deps-map {:keys [repositories no-changes?]}]
   (let [deps-edn (cond-> {:deps deps-map}
                    repositories (assoc :mvn/repos repositories))
         antq-deps (dep.clojure/extract-deps "" (pr-str deps-edn))
         antq-options {:reporter report/no-output-reporter
                       :no-changes no-changes?}]

     (core/antq antq-options antq-deps))))

(comment
  (outdated-deps '{org.clojure/clojure {:mvn/version "1.8.0"}}
                 {:no-changes? true}))
