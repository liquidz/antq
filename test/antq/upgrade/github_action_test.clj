(ns antq.upgrade.github-action-test
  (:require
   [antq.constant.github-action :as const.gh-action]
   [antq.dep.github-action :as dep.gha]
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade :as upgrade]
   [antq.upgrade.github-action]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def ^:private dummy-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "foo/bar"
                      :latest-version "v9.0.0"
                      :file (io/resource "dep/github_action.yml")
                      :extra {const.gh-action/type-key "uses"}}))

(def ^:private dummy-not-supported-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "bar/baz"
                      :latest-version "v9.0.0"
                      :file (io/resource "dep/github_action.yml")}))

(def ^:private dummy-clojure-cli-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "clojure/brew-install"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/github_action_third_party.yml")
                      :extra {const.gh-action/type-key "DeLaGuardo/setup-clojure"}}))

(def ^:private dummy-leiningen-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "technomancy/leiningen"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/github_action_third_party.yml")
                      :extra {const.gh-action/type-key "DeLaGuardo/setup-clojure"}}))

(def ^:private dummy-boot-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "boot-clj/boot"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/github_action_third_party.yml")
                      :extra {const.gh-action/type-key "DeLaGuardo/setup-clojure"}}))

(def ^:private dummy-clj-kondo-dep
  (r/map->Dependency {:project :github-action
                      :type :java
                      :name "clj-kondo/clj-kondo"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/github_action_third_party.yml")
                      :extra {const.gh-action/type-key "DeLaGuardo/setup-clj-kondo"}}))

(def ^:private dummy-graalvm-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "graalvm/graalvm-ce-builds"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/github_action_third_party.yml")
                      :extra {const.gh-action/type-key "DeLaGuardo/setup-graalvm"}}))

(def ^:private dummy-cljstyle-dep
  (r/map->Dependency {:project :github-action
                      :type :github-tag
                      :name "greglook/cljstyle"
                      :latest-version "9.0.0"
                      :file (io/resource "dep/github_action_third_party.yml")
                      :extra {const.gh-action/type-key "0918nobita/setup-cljstyle"}}))

(t/deftest upgrade-dep-test
  (t/testing "supported"
    (let [from-deps (->> (:file dummy-dep)
                         (slurp)
                         (dep.gha/extract-deps ""))
          to-deps (->> dummy-dep
                       (upgrade/upgrader)
                       (dep.gha/extract-deps ""))]
      (t/is (= #{{:name "foo/bar" :version {:- "v1.0.0" :+ "v9.0.0"}}}
               (h/diff-deps from-deps to-deps)))))

  (t/testing "not supported"
    (t/is (nil? (upgrade/upgrader dummy-not-supported-dep)))))

(t/deftest upgrade-third-party-dep-test
  (t/testing "clojure"
    (t/testing "clojure cli"
      (let [from-deps (->> (:file dummy-clojure-cli-dep)
                           (slurp)
                           (dep.gha/extract-deps ""))
            to-deps (->> dummy-clojure-cli-dep
                         (upgrade/upgrader)
                         (dep.gha/extract-deps ""))]
        (t/is (= #{{:name "clojure/brew-install" :version {:- 1 :+ "9.0.0"}}}
                 (h/diff-deps from-deps to-deps)))))

    (t/testing "leiningen"
      (let [from-deps (->> (:file dummy-leiningen-dep)
                           (slurp)
                           (dep.gha/extract-deps ""))
            to-deps (->> dummy-leiningen-dep
                         (upgrade/upgrader)
                         (dep.gha/extract-deps ""))]
        (t/is (= #{{:name "technomancy/leiningen" :version {:- 2 :+ "9.0.0"}}}
                 (h/diff-deps from-deps to-deps)))))

    (t/testing "boot"
      (let [from-deps (->> (:file dummy-boot-dep)
                           (slurp)
                           (dep.gha/extract-deps ""))
            to-deps (->> dummy-boot-dep
                         (upgrade/upgrader)
                         (dep.gha/extract-deps ""))]
        (t/is (= #{{:name "boot-clj/boot" :version {:- 3 :+ "9.0.0"}}}
                 (h/diff-deps from-deps to-deps))))))

  (t/testing "clj-kondo"
    (let [from-deps (->> (:file dummy-clj-kondo-dep)
                         (slurp)
                         (dep.gha/extract-deps ""))
          to-deps (->> dummy-clj-kondo-dep
                       (upgrade/upgrader)
                       (dep.gha/extract-deps ""))]
      (t/is (= #{{:name "clj-kondo/clj-kondo" :version {:- "5" :+ "9.0.0"}}}
               (h/diff-deps from-deps to-deps)))))

  (t/testing "graalvm"
    (let [from-deps (->> (:file dummy-graalvm-dep)
                         (slurp)
                         (dep.gha/extract-deps ""))
          to-deps (->> dummy-graalvm-dep
                       (upgrade/upgrader)
                       (dep.gha/extract-deps ""))]
      (t/is (= #{{:name "graalvm/graalvm-ce-builds" :version {:- "6" :+ "9.0.0"}}}
               (h/diff-deps from-deps to-deps)))))

  (t/testing "cljstyle"
    (let [from-deps (->> (:file dummy-cljstyle-dep)
                         (slurp)
                         (dep.gha/extract-deps ""))
          to-deps (->> dummy-cljstyle-dep
                       (upgrade/upgrader)
                       (dep.gha/extract-deps ""))]
      (t/is (= #{{:name "greglook/cljstyle" :version {:- "7" :+ "9.0.0"}}}
               (h/diff-deps from-deps to-deps))))))
