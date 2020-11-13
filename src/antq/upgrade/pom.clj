(ns antq.upgrade.pom
  (:require
   [antq.upgrade :as upgrade]
   [clojure.data.xml :as xml]
   [clojure.data.zip :as d.zip]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.zip :as zip]))

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

(defn- edit-version
  [loc new-version]
  (loop [loc (zip/right loc)]
    (if (d.zip/rightmost? loc)
      loc
      (if (= "version" (some-> (zip/node loc) :tag name))
        (zip/edit loc #(assoc % :content [new-version]))
        (recur (zip/right loc))))))

(defn upgrade-dep
  [loc version-checked-dep]
  (let [[group-id artifact-id] (str/split (:name version-checked-dep) #"/" 2)]
    (loop [loc loc]
      (if (zip/end? loc)
        (zip/root loc)
        (if (target-dependency? loc group-id artifact-id)
          (recur (edit-version loc (:latest-version version-checked-dep)))
          (recur (zip/next loc)))))))

(defmethod upgrade/upgrader :pom
  [version-checked-dep]
  (-> (:file version-checked-dep)
      (io/input-stream)
      (xml/parse)
      (zip/xml-zip)
      (upgrade-dep version-checked-dep)
      (xml/indent-str)))
