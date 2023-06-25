(ns antq.relocate
  (:require
   [antq.util.dep :as u.dep]
   [antq.util.maven :as u.mvn]))

(def ^:private disallowed-unverified-deps-map
  {"antq/antq" "com.github.liquidz/antq"
   "seancorfield/depstar" "com.github.seancorfield/depstar"
   "seancorfield/next.jdbc" "com.github.seancorfield/next.jdbc"})

(defn- relocated-dep
  [base-dep new-location]
  (assoc base-dep
         :version (:name base-dep)
         :latest-name new-location))

(defn- get-relocated-deps-name*
  [dep]
  (when-let [{:keys [group-id artifact-id]} (some-> (u.dep/get-pom-path dep)
                                                    (u.mvn/read-pom)
                                                    (u.mvn/get-distribution-management)
                                                    (u.mvn/get-relocation))]
    (str group-id "/" artifact-id)))

(def ^:private get-relocated-deps-name
  (memoize get-relocated-deps-name*))

(defmulti get-relocation
  (fn [dep]
    (:type dep)))
(defmethod get-relocation :default [_] nil)

(defmethod get-relocation :java
  [dep]
  (get-relocated-deps-name dep))

(defn relocated-deps
  [deps]
  (concat
   ;; Unverified
   (keep #(when-let [verified-name (and (= :java (:type %))
                                        (get disallowed-unverified-deps-map (:name %)))]
            (relocated-dep % verified-name))
         deps)
   ;; Relocated
   (keep #(when-let [relocated-name (get-relocation %)]
            (relocated-dep % relocated-name))
         deps)))

(comment
  (get-relocated-deps-name* {:name "org.clojure/clojure" :version "1.11.1"})
  (get-relocated-deps-name* {:name "mysql/mysql-connector-java" :version "8.0.33"}))
