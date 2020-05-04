(defproject foo "0.1.0-SNAPSHOT"
  :dependencies [[foo/core "1.0.0"]
                 [bar "2.0.0"]]

  :profiles
  {:foo
   {:dependencies [[baz "3.0.0"]]}})
