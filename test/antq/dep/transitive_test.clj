(ns antq.dep.transitive-test
  (:require
   [antq.dep.transitive :as sut]
   [antq.record :as r]
   [clojure.test :as t]
   [clojure.tools.deps :as deps]))

(def ^:private dummy-dep
  {:type :java
   :name "foo/alice"
   :version "1.0.0"})

(defn- mock-resolve-deps
  [deps-map _]
  (condp = (ffirst (:deps deps-map))
    'foo/alice {'foo/bob {:deps/manifest :mvn
                          :mvn/version "2.0.0"
                          :dependents ['foo/alice]}}
    'foo/bob {'foo/charlie {:deps/manifest :deps
                            :git/sha "1234567890"
                            :git/url "https://example.com/charlie"
                            :dependents ['foo/bob]}
              'foo/dave {:deps/manifest :deps
                         :git/tag "v3"
                         :git/sha "0987654321"
                         :git/url "https://example.com/dave"
                         :dependents ['foo/bob]}}
    {}))

(t/deftest resolve-transitive-deps-test
  (with-redefs [deps/resolve-deps mock-resolve-deps]
    (t/is (= [(r/map->Dependency {:type :java
                                  :name "foo/bob"
                                  :version "2.0.0"
                                  :file ""
                                  :parent "foo/alice"})
              (r/map->Dependency {:type :git-sha
                                  :name "foo/charlie"
                                  :version "1234567890"
                                  :file ""
                                  :parent "foo/bob"
                                  :extra {:url "https://example.com/charlie"}})
              (r/map->Dependency {:type :git-tag-and-sha
                                  :name "foo/dave"
                                  :version "v3"
                                  :file ""
                                  :parent "foo/bob"
                                  :extra {:sha "0987654321"
                                          :url "https://example.com/dave"}})]
             (->> (sut/resolve-transitive-deps [dummy-dep])
                  (sort-by :name))))))
