(ns antq.upgrade.pom
  (:require
   [antq.log :as log]
   [antq.upgrade :as upgrade]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.zip :as zip]))

(defn- find-version [loc]
  (loop [loc loc]
    (if (= "version" (some-> (zip/node loc) :tag name))
      loc
      (recur (zip/right loc)))))

(defn- target-dependency?
  [loc group-id artifact-id]
  (let [{:keys [tag content]} (zip/node loc)]
    (if (and tag
             (= "groupId" (name tag))
             (= [group-id] content))
      (->> (zip/rights loc)
           (filter #(and (= "artifactId" (some-> % :tag name))
                         (= [artifact-id] (:content %))))
           (seq)
           (some?))
      false)))

(defn- property-version?
  [loc]
  (let [loc (find-version loc)
        {:keys [content]} (zip/node loc)
        current-version (first content)]
    (some? (re-seq #"\$\{.+?\}" current-version))))

(defn- edit-version
  [loc new-version]
  (-> loc
      (find-version)
      (zip/edit #(assoc % :content [new-version]))))

(defn upgrade-dep
  [loc version-checked-dep]
  (let [[group-id artifact-id] (str/split (:name version-checked-dep) #"/" 2)]
    (loop [loc loc]
      (if (zip/end? loc)
        (zip/root loc)
        (if (target-dependency? loc group-id artifact-id)
          (if (property-version? loc)
            (do (log/info
                 (format "Skipped to upgrade %s because the version is managed by properties."
                         (:name version-checked-dep)))
                (recur (zip/next loc)))
            (recur (edit-version loc (:latest-version version-checked-dep))))
          (recur (zip/next loc)))))))

(defmethod upgrade/upgrader :pom
  [version-checked-dep]
  (-> (:file version-checked-dep)
      (io/input-stream)
      (xml/parse :skip-whitespace true)
      (zip/xml-zip)
      (upgrade-dep version-checked-dep)
      (xml/indent-str)))
