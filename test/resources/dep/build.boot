(def foo ::foo)

(set-env!
  :dependencies '[[foo/core "1.0.0"]
                  [bar "2.0.0"]
                  [baz "3.0.0" :scope "test"]])

(def bar ::bar)
