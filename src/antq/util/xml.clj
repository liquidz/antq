(ns antq.util.xml)

(defn xml-ns [content]
  (-> (if (sequential? content)
        (first (filter map? content))
        content)
      :tag
      namespace))

(defn get-tags [tag content]
  (let [xns (xml-ns content)]
    (->> content
         (filter (comp #{(keyword xns (name tag))} :tag)))))

(defn get-value
  [tag content]
  (->> content
       (get-tags tag)
       first
       :content
       first))

(defn get-values
  [tags content]
  (map #(get-value % content) tags))

(defn get-attribute
  [tag attr content]
  (->> content
       (get-tags tag)
       first
       :attrs
       attr))
