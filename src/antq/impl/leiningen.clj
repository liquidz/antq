(ns antq.impl.leiningen
  (:require
   [antq.record :as r]
   [clojure.walk :as walk]))

(defn extract-deps
  [project-clj-content-str]
  (let [deps (atom [])]
    (walk/postwalk (fn [form]
                     (when (and (sequential? form)
                                (= :dependencies (first form)))
                       (swap! deps concat (second form)))
                     form)
                   (read-string (str "(list " project-clj-content-str " )")))
    (for [[dep-name version] @deps]
      (r/map->Dependency {:type :java :name  (str dep-name) :version version}))))


