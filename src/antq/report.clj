(ns antq.report
  (:require
   [antq.log :as log]))

(defmulti reporter
  (fn [_deps options]
    (:reporter options)))

(defmethod reporter :default
  [_ options]
  (log/error (str "Unknown reporter: " (:reporter options))))

(defmulti init-progress
  (fn [_deps options]
    (:reporter options)))

(defmethod init-progress :default [_ _] nil)

(defmulti run-progress
  (fn [_dep options]
    (:reporter options)))

(defmethod run-progress :default [_ _] nil)
