(def foo ::foo)

(set-env!
  :dependencies '[[foo/core "1.0.0"]
                  [bar "2.0.0"]
                  [baz "3.0.0" :scope "test"]
                  [ver-not-string :version]
                  [ver-empty ""]])

(set-env!
  :repositories #(conj % '["antq-test" {:url "s3://antq-repo/"}]))

(def bar ::bar)
