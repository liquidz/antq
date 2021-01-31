(ns antq.dep.github-action
  (:require
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [antq.util.ver :as u.ver]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk])
  (:import
   java.io.File))

(defn- sha-1?
  [s]
  (some? (and (re-seq #"^[a-fA-F0-9]+$" s)
              (#{40 7} (count s)))))

(defn- name->url
  [^String name]
  (if (= 0 (.indexOf name "https://"))
    name
    (str "https://github.com/" name ".git")))

(defn- extract-type-and-version
  [name version]
  (if (sha-1? version)
    {:type :git-sha
     :version version
     :extra {:url (name->url name)}}
    {:type :github-tag
     :version (u.ver/normalize-version version)}))

(defn extract-deps
  [file-path workflow-content-str]
  (let [deps (atom [])]
    (walk/prewalk (fn [form]
                    (when (and (vector? form)
                               (= :uses (first form)))
                      (swap! deps conj (second form)))
                    form)
                  (yaml/parse-string workflow-content-str))
    (for [d @deps
          :let [[name version] (str/split d #"@" 2)]
          :when (seq version)]
      (-> {:project :github-action
           :file file-path
           :name name}
          (merge (extract-type-and-version name version))
          (r/map->Dependency)))))

(defn load-deps
  ([] (load-deps "."))
  ([dir]
   (let [dir-file (io/file dir ".github" "workflows")]
     (when (.isDirectory dir-file)
       (->> (file-seq dir-file)
            (filter #(.isFile ^File %))
            (mapcat #(extract-deps (u.dep/relative-path %)
                                   (slurp %))))))))
