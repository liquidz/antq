(ns antq.dep.github-action.third-party
  (:require
   [antq.record :as r]
   [clojure.string :as str]))

(defn- map->Dependency
  [m]
  (-> m
      (assoc :type :github-tag
             :project :github-action)
      (r/map->Dependency)))


(defmulti detect
  (fn [form]
    (when (and (map? form)
               (:uses form))
      (first (str/split (:uses form) #"@")))))

(defmethod detect :default [_] nil)

(defmethod detect "DeLaGuardo/setup-clojure"
  [form]
  (->> (:with form)
       (keep (fn [[k v]]
               (case k
                 (:tools-deps :cli) {:name "clojure/brew-install" :version v}
                 :lein {:name "technomancy/leiningen" :version v}
                 :boot {:name "boot-clj/boot" :version v}
                 nil)))
       (map #(-> (assoc % :type :github-tag)
                 (r/map->Dependency)))))

(defmethod detect "DeLaGuardo/setup-clj-kondo"
  [form]
  (when-let [v (get-in form [:with :version])]
    [(r/map->Dependency
      {:name "clj-kondo/clj-kondo"
       :version v
       :type :java})]))

(defmethod detect "DeLaGuardo/setup-graalvm"
  [form]
  (when-let [v (get-in form [:with :graalvm-version])]
    [(r/map->Dependency
      {:name "graalvm/graalvm-ce-builds"
       :version (str/replace v #"\.java\d+$" "")
       :type :github-tag})]))

(defmethod detect "0918nobita/setup-cljstyle"
  [form]
  (when-let [v (get-in form [:with :cljstyle-version])]
    [(r/map->Dependency
      {:name "greglook/cljstyle"
       :version v
       :type :github-tag})]))
