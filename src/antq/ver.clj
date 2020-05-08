(ns antq.ver
  (:require
   [clojure.string :as str]))

(def ^:private under-development-keywords
  #{"alpha" "beta" "rc"})

(defn under-devleopment?
  [s]
  (let [l (str/lower-case s)]
    (some? (some #(str/includes? l %) under-development-keywords))))

(defmulti get-sorted-versions :type)
(defmethod get-sorted-versions :default
  [dep]
  (throw (ex-info "Unknown dependency type" dep)))
