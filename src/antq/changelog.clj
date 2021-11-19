(ns antq.changelog
  (:require
   [antq.util.maven :as u.mvn]
   [antq.util.url :as u.url]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.gitlibs :as gitlibs]))

(def changelog-filenames
  #{"changelog.adoc"
    "changelog.md"
    "changelog.or"
    "changes.md"})

(defn- silent-procure
  [url lib rev]
  (binding [*err* (java.io.StringWriter.)]
    (gitlibs/procure url lib rev)))

(defn- get-root-file-names
  [url lib rev]
  (when-let [dir (silent-procure url lib rev)]
    (seq (.list (io/file dir)))))

(defmulti get-git-url
  (fn [version-checked-dep]
    (:type version-checked-dep)))

(defmethod get-git-url :default
  [_dep]
  nil)

(defmethod get-git-url :git-sha
  [dep]
  (get-in dep [:extra :url]))

(defmethod get-git-url :github-tag
  [dep]
  (format "https://github.com/%s"
          (str/join "/" (take 2 (str/split (:name dep) #"/")))))

(defmethod get-git-url :java
  [dep]
  (u.mvn/get-scm-url-by-version-checked-dep dep))

(defn get-changelog-url
  [{:as version-checked-dep :keys [latest-version]}]
  (let [url (get-git-url version-checked-dep)
        file-names (when url
                     (get-root-file-names
                      url
                      (symbol (:name version-checked-dep))
                      latest-version))
        changelog (when file-names
                    (some #(and (contains? changelog-filenames (str/lower-case %)) %)
                          file-names))]
    (when changelog
      (str (u.url/ensure-git-https-url url)
           "blob/" latest-version "/" changelog))))




(comment
  (require 'antq.record)
  (require 'antq.util.fn)
  (get-changelog-url
   (antq.record/map->Dependency {:type :git-sha
                                 :name "com.github.liquidz/antq"
                                 :extra {:url "https://github.com/liquidz/antq.git"}
                                 :version "1"
                                 :latest-version "9b664f4b05be0d03366d418b1c4b50c5329726b4"}))

  (get-changelog-url
   (antq.record/map->Dependency {:type :java
                                 :name "com.github.liquidz/antq"
                                 :version "1.1.0"
                                 :latest-version "1.2.0"
                                 :repositories u.mvn/default-repos})))
