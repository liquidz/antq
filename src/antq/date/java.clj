(ns antq.date.java
  (:require
   [antq.date :as date]
   [antq.util.dep :as u.dep]
   [antq.util.maven :as u.maven]))

(defmethod date/get-last-updated-at :java
  [dep _options]
  (u.maven/get-last-updated
   (:name dep)
   (u.dep/repository-opts dep)))
