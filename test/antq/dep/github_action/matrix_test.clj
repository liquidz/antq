(ns antq.dep.github-action.matrix-test
  (:require
   [antq.dep.github-action.matrix :as sut]
   [antq.record :as r]
   [clojure.test :as t]))

(def ^:private dummy-parsed-yaml
  {:jobs
   {:job1 {:strategy {:matrix {:foo ["1" "2"]}}}
    :job2 {:strategy {:matrix {:foo ["3" "4"]}}}}})

(def ^:private dummy-deps
  [(r/map->Dependency {:name "foo" :version "${{matrix.foo}}"})
   (r/map->Dependency {:name "bar" :version "9"})])

(t/deftest expand-matrixed-value-test
  (t/is (= #{(r/map->Dependency {:name "foo" :version "1" :only-newest-version? true})
             (r/map->Dependency {:name "foo" :version "2" :only-newest-version? true})
             (r/map->Dependency {:name "bar" :version "9"})}
           (set (sut/expand-matrix-value
                 dummy-parsed-yaml
                 :job1
                 dummy-deps))))
  (t/is (= #{(r/map->Dependency {:name "foo" :version "3" :only-newest-version? true})
             (r/map->Dependency {:name "foo" :version "4" :only-newest-version? true})
             (r/map->Dependency {:name "bar" :version "9"})}
           (set (sut/expand-matrix-value
                 dummy-parsed-yaml
                 :job2
                 dummy-deps)))))
