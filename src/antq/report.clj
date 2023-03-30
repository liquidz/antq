(ns antq.report
  (:require
   [antq.log :as log]))

(def no-output-reporter "__NO_OUTPUT__")

(defmulti reporter
  (fn [_deps options]
    (:reporter options)))

(defmethod reporter :default
  [_ options]
  (log/error (str "Unknown reporter: " (:reporter options))))

(defmethod reporter no-output-reporter
  [_ _]
  nil)

(defmulti init-progress
  (fn [_deps options]
    (:reporter options)))

(defmethod init-progress :default [_ _] nil)

(defmulti run-progress
  (fn [_dep options]
    (:reporter options)))

(defmethod run-progress :default [_ _] nil)

(defmulti deinit-progress
  (fn [_dep options]
    (:reporter options)))

(defmethod deinit-progress :default [_ _] nil)
