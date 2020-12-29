(defproject foo "0.1.0-SNAPSHOT"
  :dependencies [[foo/core "1.0.0"]
                 ^:inline-dep [bar "2.0.0" :exclusions [org.clojure/clojure]]
                 [ver-not-string :version]
                 [ver-empty ""]]

  :profiles
  {:foo
   {:dependencies [[baz "3.0.0"]]}

   :bar
   [;; same artifact name, but different version number
    :dependencies [[foo/core "1.1.0"]]]}

  :repositories [["antq-test" {:url "s3://antq-repo/"}]
                 ["str-test" "https://example.com"]])
