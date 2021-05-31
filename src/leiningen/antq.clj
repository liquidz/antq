(ns leiningen.antq
  (:require
   [antq.core]
   [antq.dep.leiningen :as dep.lein]
   [antq.record :as r]
   [leiningen.core.main]))

(defn antq
  "Leiningen plugin.

  Checks project.clj via full Leiningen evaluation. Does not check any other sources (deps.edn, etc).

  For the time being it merely checks for outdated dependencies;
  it doesn't support the `:upgrade` option because it cannot always know what to fix
  (in face of eval, profiles, plugins/middleware)."
  [{:keys [dependencies managed-dependencies repositories]
    {:keys [error-format reporter upgrade] :as antq-options} :antq}]
  (let [repos (dep.lein/normalize-repositories repositories)
        options (cond-> antq-options
                  (and (not error-format)
                       (not reporter)) (assoc :reporter "table"))
        _ (when upgrade
            (assert false ":upgrade option not supported under the Lein plugin."))
        outdated (->> dependencies
                      (into managed-dependencies)
                      (distinct)
                      (keep (fn [[dep-name version]]
                              (when (dep.lein/acceptable-version? version)
                                (r/map->Dependency {:project      :leiningen
                                                    :type         :java
                                                    :file         "project.clj"
                                                    :name         (dep.lein/normalize-name dep-name)
                                                    :version      version
                                                    :repositories repos}))))
                      (antq.core/antq options)
                      (seq))]
    (binding [leiningen.core.main/*exit-process?* true]
      (leiningen.core.main/exit (if outdated
                                  1
                                  0)))))
