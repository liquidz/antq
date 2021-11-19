(ns antq.diff.java
  (:require
   [antq.diff :as diff]
   [antq.log :as log]
   [antq.util.git :as u.git]
   [antq.util.maven :as u.mvn]
   [antq.util.url :as u.url]
   [clojure.string :as str]))

(defmethod diff/get-diff-url :java
  [{:as dep :keys [version latest-version]}]
  (when (and version latest-version)
    (when-let [url (u.mvn/get-scm-url-by-version-checked-dep dep)]
      (cond
        (str/starts-with? url "https://github.com/")
        (let [tags (u.git/tags-by-ls-remote url)
              current (first (filter #(str/includes? % version) tags))
              latest (or (first (filter #(str/includes? % latest-version) tags))
                         ;; If there isn't a tag for latest version
                         "head")]
          (if current
            (format "%scompare/%s...%s"
                    (u.url/ensure-tail-slash url)
                    current
                    latest)
            (do (log/warning (str "The tag for current version is not found: " url))
                ;; not diff, but URL is useful for finding the differences.
                nil)))

        :else
        (do (log/warning (str "Diff is not supported for " url))
            ;; not diff, but URL is useful for finding the differences.
            nil)))))
