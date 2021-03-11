(ns antq.record)

(defrecord Dependency
  [;; Dependency type keyword
   ;; e.g. :java, :git-sha or :github-tag
   type
   ;; File path for project configuration file
   file
   ;; Dependency name
   ;; e.g. "org.clojure/clojure", "medley/medley"
   name
   ;; Current version string
   version
   ;; Latest version string (Nullable)
   latest-version
   ;; Additional Maven repositories (Nullable)
   ;; e.g. {"nexus-snapshots" {:url "http://localhost:8081/repository/maven-snapshots/"}}
   repositories
   ;; Project type keyword
   ;; e.g. :clojure, :leiningen, :shadow-cljs and so on.
   project
   ;; Key to indicate which parameter is outdated
   ;; e.g. :version or :name
   outdated-target])

(defn version-outdated?
  [dep]
  (= :version (:outdated-target dep)))
