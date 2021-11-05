;; Warn on Clojure 1.7.0 or earlier
(let [{:keys [major minor]} *clojure-version*]
  (when-not (or (and (= major 1) (>= minor 9))
                (> major 1))
    (.println ^java.io.PrintWriter *err* "antq requires Clojure 1.9.0 or later.")
    (System/exit 1)))

(ns antq.core
  (:gen-class)
  (:require
   [antq.dep.babashka :as dep.bb]
   [antq.dep.boot :as dep.boot]
   [antq.dep.clojure :as dep.clj]
   [antq.dep.github-action :as dep.gh-action]
   [antq.dep.gradle :as dep.gradle]
   [antq.dep.leiningen :as dep.lein]
   [antq.dep.pom :as dep.pom]
   [antq.dep.shadow :as dep.shadow]
   [antq.diff :as diff]
   [antq.diff.git-sha]
   [antq.diff.github-tag]
   [antq.diff.java]
   [antq.log :as log]
   [antq.record :as r]
   [antq.report :as report]
   [antq.report.edn]
   [antq.report.format]
   [antq.report.json]
   [antq.report.table]
   [antq.upgrade :as upgrade]
   [antq.upgrade.boot]
   [antq.upgrade.clojure]
   [antq.upgrade.leiningen]
   [antq.upgrade.pom]
   [antq.upgrade.shadow]
   [antq.ver :as ver]
   [antq.ver.git-sha]
   [antq.ver.git-tag-and-sha]
   [antq.ver.github-tag]
   [antq.ver.java]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [version-clj.core :as version]))

(defn- concat-assoc-fn
  [opt k v]
  (update opt k concat (str/split v #":")))

(def ^:private supported-reporter
  (->> (methods report/reporter)
       (keys)
       (filter string?)
       (set)))

(def ^:private skippable
  #{"boot"
    "clojure-cli"
    "github-action"
    "gradle"
    "pom"
    "shadow-cljs"
    "leiningen"
    "babashka"})

(def ^:private disallowed-unverified-deps-map
  {"antq/antq" "com.github.liquidz/antq"
   "seancorfield/depstar" "com.github.seancorfield/depstar"
   "seancorfield/next.jdbc" "com.github.seancorfield/next.jdbc"})

(def ^:private only-newest-version-dep-names
  #{"org.clojure/clojure"})

(def cli-options
  [[nil "--exclude=EXCLUDE" :default [] :assoc-fn concat-assoc-fn]
   [nil "--focus=FOCUS" :default [] :assoc-fn concat-assoc-fn]
   [nil "--skip=SKIP" :default [] :assoc-fn concat-assoc-fn
    :validate [#(skippable %) (str "Must be one of [" (str/join ", " skippable) "]")]]
   [nil "--error-format=ERROR_FORMAT" :default nil]
   [nil "--reporter=REPORTER" :default "table"
    :validate [#(supported-reporter %) (str "Must be one of [" (str/join ", " supported-reporter) "]")]]
   ["-d" "--directory=DIRECTORY" :default ["."] :assoc-fn concat-assoc-fn]
   [nil "--upgrade"]
   [nil "--verbose"]
   [nil "--force"]])

(defn skip-artifacts?
  [dep options]
  (let [exclude-artifacts (set (:exclude options []))
        focus-artifacts (set (:focus options []))]
    (cond
      ;; `focus` is prefer than `exclude`
      (seq focus-artifacts)
      (not (contains? focus-artifacts (:name dep)))

      :else
      (contains? exclude-artifacts (:name dep)))))

(defn remove-skipping-versions
  [versions dep-name options]
  (let [skip-vers (->> (:exclude options)
                       (map #(str/split % #"@" 2))
                       (filter #(= dep-name (first %)))
                       (keep second)
                       (set))]
    (remove skip-vers versions)))

(defn using-release-version?
  [dep]
  (contains? #{"RELEASE" "master" "main" "latest"} (:version dep)))

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
        (log/info))))

(defn- assoc-latest-version
  [dep options]
  (let [vers (cond->> (:_versions dep)
               (not (ver/under-devleopment? (:version dep)))
               (drop-while ver/under-devleopment?))
        vers (remove-skipping-versions vers (:name dep) options)
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
                                 (pmap assoc-versions))
        assoc-latest-version* #(assoc-latest-version % options)]
    (->> org-deps
         (pmap #(complete-versions-by % uniq-deps-with-vers))
         (map (comp dissoc-no-longer-used-keys
                    assoc-latest-version*))
         (remove ver/latest?))))

(defn assoc-diff-url
  [version-checked-dep]
  (if-let [url (diff/get-diff-url version-checked-dep)]
    (assoc version-checked-dep :diff-url url)
    version-checked-dep))

(defn unverified-deps
  [deps]
  (keep #(when-let [verified-name (and (= :java (:type %))
                                       (get disallowed-unverified-deps-map (:name %)))]
           (assoc %
                  :version (:name %)
                  :latest-version nil
                  :latest-name verified-name))
        deps))

(defn exit
  [outdated-deps]
  (System/exit (if (seq outdated-deps) 1 0)))

(defn fetch-deps
  [options]
  (let [skip (set (:skip options))]
    (mapcat #(concat
              (when-not (skip "boot") (dep.boot/load-deps %))
              (when-not (skip "clojure-cli") (dep.clj/load-deps %))
              (when-not (skip "github-action") (dep.gh-action/load-deps %))
              (when-not (skip "pom") (dep.pom/load-deps %))
              (when-not (skip "shadow-cljs") (dep.shadow/load-deps %))
              (when-not (skip "leiningen") (dep.lein/load-deps %))
              (when-not (skip "babashka") (dep.bb/load-deps %))
              (when-not (skip "gradle") (dep.gradle/load-deps %)))
            (distinct (:directory options)))))

(defn mark-only-newest-version-flag
  [deps]
  (map #(cond-> %
          (contains? only-newest-version-dep-names (:name %))
          (assoc :only-newest-version? true))
       deps))

(defn unify-deps-having-only-newest-version-flag
  "Keep only the newest version in the same file if `:only-newest-version?` flag is marked."
  [deps]
  (let [other-deps (remove :only-newest-version? deps)]
    (->> deps
         (filter :only-newest-version?)
         (group-by :file)
         (map (fn [[_ deps]]
                (->> deps
                     (sort (fn [a b] (version/version-compare (:version b) (:version a))))
                     first)))
         (concat other-deps))))

(defn antq
  [options deps]
  (let [deps (->> deps
                  (mark-only-newest-version-flag)
                  (unify-deps-having-only-newest-version-flag))
        outdated (->> (outdated-deps deps options)
                      (map assoc-diff-url)
                      (concat (unverified-deps deps)))]
    (report/reporter outdated options)
    outdated))

(defn main*
  [options errors]
  (let [options (cond-> options
                  ;; Force "format" reporter when :error-format is specified
                  (some?  (:error-format options)) (assoc :reporter "format"))
        deps (and (not errors)
                  (fetch-deps options))]
    (cond
      errors
      (do (doseq [e errors]
            (log/error e))
          (System/exit 1))

      (seq deps)
      (let [outdated (antq options deps)]
        (cond-> outdated
          (:upgrade options)
          (upgrade/upgrade! (or (:force options) false))

          true
          (exit)))

      :else
      (do (log/info "No project file")
          (System/exit 1)))))

(defn -main
  [& args]
  (let [{:keys [options errors]} (cli/parse-opts args cli-options)]
    (binding [log/*verbose* (:verbose options false)]
      (main* options errors))))
