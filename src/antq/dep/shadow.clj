(ns antq.dep.shadow
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.walk :as walk]))

(def ^:private project-file "shadow-cljs.edn")

(defn- getenv
  [k]
  (System/getenv k))

(defn- read-env
  [arg]
  (let [[envname & opts] (cond-> arg
                           (not (sequential? arg)) vector)
        option (when (seq opts)
                 (cond->> opts
                   (not (keyword? (first opts))) (cons :default)
                   true (apply hash-map)))]
    (or (getenv envname)
        (get option :default))))

(def ^:private readers
  "c.f. https://github.com/thheller/shadow-cljs/blob/2.10.21/src/main/shadow/cljs/devtools/config.clj#L102-L107"
  {:readers
   {'shadow/env read-env
    'env read-env}})

(defn extract-deps
  [file-path shadow-cljs-edn-content-str]
  (let [deps (atom [])]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (= :dependencies (first form)))
                       (swap! deps concat (second form)))
                     form)
                   (edn/read-string readers shadow-cljs-edn-content-str))
    (for [[dep-name version] @deps
          :when (and (string? version) (seq version))]
      (r/map->Dependency {:type :java
                          :file file-path
                          :name  (str dep-name)
                          :version version}))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [file (io/file dir project-file)]
     (when (.exists file)
       (extract-deps (u.dep/relative-path file)
                     (slurp file))))))
