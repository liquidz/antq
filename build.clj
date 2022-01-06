(ns build
  (:require
   [clojure.tools.build.api :as b]
   [clojure.xml :as xml]
   [deps-deploy.deps-deploy :as deploy]))

(def ^:private class-dir "target/classes")
(def ^:private jar-file "target/antq.jar")
(def ^:private lib 'com.github.liquidz/antq)
(def ^:private main 'antq.core)
(def ^:private pom-file "./pom.xml")
(def ^:private uber-file "target/antq-standalone.jar")

(defn- get-basis
  [opt]
  (-> (select-keys opt [:aliases])
      (merge {:project "deps.edn"})
      (b/create-basis)))

(defn- get-current-version
  [pom-file-path]
  (->> (xml/parse pom-file-path)
       (xml-seq)
       (some #(and (= :version (:tag %)) %))
       (:content)
       (first)))

(defn pom
  [arg]
  (let [basis (or (:basis arg) (get-basis arg))
        lib' (or (:lib arg) lib)]
    (b/write-pom {:basis basis
                  :class-dir class-dir
                  :lib lib'
                  :version (get-current-version pom-file)
                  :src-dirs ["src"]})
    (when (:copy? arg true)
      (b/copy-file {:src (b/pom-path {:lib lib' :class-dir class-dir})
                    :target pom-file}))))

(defn jar
  [arg]
  (let [basis (get-basis {})
        arg (assoc arg :basis basis)]
    (pom arg)
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})))

(defn uberjar
  [arg]
  (let [;; NOTE: To include org.slf4j/slf4j-nop
        basis (get-basis {:aliases [:nop]})
        arg (assoc arg
                   :basis basis
                   :copy? false)]
    (pom arg)
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    ;; NOTE: does not contain src/leiningen
                    :src-dirs ["src/antq"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main main})))

(defn install
  [arg]
  (jar arg)
  (deploy/deploy {:artifact jar-file
                  :installer :local}))

(defn deploy
  [arg]
  (assert (and (System/getenv "CLOJARS_USERNAME")
               (System/getenv "CLOJARS_PASSWORD")))
  (jar arg)
  (deploy/deploy {:artifact jar-file
                  :installer :remote}))
