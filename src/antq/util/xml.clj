(ns antq.util.xml)

(defn get-value
  [content tag]
  (->> content
       (filter (comp #{tag} :tag))
       first
       :content
       first))

(defn get-values
  [content tags]
  (map #(get-value content %) tags))

(defn get-attribute
  [content tag attr]
  (->> content
       (filter (comp #{tag} :tag))
       first
       :attrs
       attr))
