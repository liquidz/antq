(ns antq.util.zip-test
  (:require
   [antq.util.zip :as sut]
   [clojure.test :as t]
   [rewrite-clj.zip :as z]))

(t/deftest move-to-root-test
  (let [loc (z/of-string "(foo (bar (baz)))")
        loc' (-> loc
                 (z/next)
                 (z/next)
                 (sut/move-to-root))]
    (t/is (= (z/sexpr loc)
             (z/sexpr loc')))))
