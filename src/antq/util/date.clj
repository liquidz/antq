(ns antq.util.date
  (:import
   (java.time
    ZoneId
    ZonedDateTime)
   java.time.format.DateTimeFormatter))

(def ^:private utc-zone-id
  (ZoneId/of "UTC"))

(defn yyyyMMddHHmmss
  ^ZonedDateTime [s]
  (let [formatter (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")]
    (ZonedDateTime/parse s (.withZone formatter utc-zone-id))))

(defn iso-date-time
  ^ZonedDateTime [s]
  (let [zdt (ZonedDateTime/parse s DateTimeFormatter/ISO_OFFSET_DATE_TIME)]
    (.withZoneSameInstant zdt utc-zone-id)))

(comment
  (iso-date-time "2023-05-03T09:18:13+08:00"))
