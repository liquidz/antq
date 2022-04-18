(ns antq.dep.github-action.third-party
  (:require
   [antq.constant.github-action :as const.gh-action]
   [antq.record :as r]
   [antq.util.dep :as u.dep]
   [clojure.string :as str]))

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
       (map #(-> %
                 (assoc :type :github-tag)
                 (assoc-in [:extra const.gh-action/type-key] "DeLaGuardo/setup-clojure")
                 (r/map->Dependency)))))

(defmethod detect "DeLaGuardo/setup-clj-kondo"
  [form]
  (when-let [v (get-in form [:with :version])]
    [(r/map->Dependency
      {:name "clj-kondo/clj-kondo"
       :version v
       :type :java
       :extra {const.gh-action/type-key "DeLaGuardo/setup-clj-kondo"}})]))

(defmethod u.dep/normalize-by-name "graalvm/graalvm-ce-builds"
  [dep]
  (update dep :version #(str/replace % #"\.java\d+$" "")))

(defmethod detect "DeLaGuardo/setup-graalvm"
  [form]
  (when-let [v (or
                ;; before v4.0
                (get-in form [:with :graalvm-version])
                ;; v4.0 or later
                (get-in form [:with :graalvm]))]
    [(u.dep/normalize-by-name
      (r/map->Dependency
       {:name "graalvm/graalvm-ce-builds"
        :version v
        :type :github-tag
        :extra {const.gh-action/type-key "DeLaGuardo/setup-graalvm"}}))]))

(defmethod detect "0918nobita/setup-cljstyle"
  [form]
  (when-let [v (get-in form [:with :cljstyle-version])]
    [(r/map->Dependency
      {:name "greglook/cljstyle"
       :version v
       :type :github-tag
       :extra {const.gh-action/type-key "0918nobita/setup-cljstyle"}})]))
