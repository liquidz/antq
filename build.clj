(ns build
  (:require
   [build-edn.core :as build-edn]))

(def ^:private config
  {:lib 'com.github.liquidz/antq
   :version "1.6.{{git/commit-count}}"
   :main 'antq.core
   :scm {:connection "scm:git:git://github.com/liquidz/antq.git"
         :developerConnection "scm:git:ssh://git@github.com/liquidz/antq.git"
         :url "https://github.com/liquidz/antq"}
   :documents [{:file "CHANGELOG.adoc"
                :match "Unreleased"
                :action :append-after
                :text "\n== {{version}} ({{now/yyyy}}-{{now/mm}}-{{now/dd}})"}
               {:file "README.adoc"
                :match "install com\\.github\\.liquidz/antq"
                :action :replace
                :text "clojure -Ttools install com.github.liquidz/antq '{:git/tag \"{{version}}\"}' :as antq"}]
   :github-actions? true})

(defn jar
  [m]
  (-> (merge config m)
      (build-edn/jar)))

(defn uberjar
  [m]
  (-> (merge config m)
      (assoc
       ;; NOTE: To include org.slf4j/slf4j-nop
       :aliases [:nop]
       ;; NOTE: does not contain src/leiningen
       :src-dirs ["src/antq"])
      (build-edn/uberjar)))

(defn install
  [m]
  (-> (merge config m)
      (build-edn/install)))

(defn deploy
  [m]
  (let [config (merge config m)]
    (build-edn/deploy (assoc config :lib 'antq/antq))
    (build-edn/deploy config)))

(defn update-documents
  [m]
  (-> (merge config m)
      (build-edn/update-documents)))

(defn lint
  [m]
  (-> (merge config m)
      (build-edn/lint)))
