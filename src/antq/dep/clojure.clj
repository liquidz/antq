(ns antq.dep.clojure
  "Clojure CLI"
  (:require
   [antq.record :as r]
   [clojure.edn :as edn]
   [clojure.walk :as walk]))

(defn extract-deps
  [deps-edn-content-str]
  (let [deps (atom {})]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (#{:deps :extra-deps} (first form)))
                       (swap! deps merge (second form)))
                     form)
                   (edn/read-string deps-edn-content-str))
    (for [[dep-name {:mvn/keys [version]}] @deps]
      (r/map->Dependency {:type :java :name  (str dep-name) :version version}))))


