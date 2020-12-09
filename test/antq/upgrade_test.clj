(ns antq.upgrade-test
  (:require
   [antq.record :as r]
   [antq.upgrade :as sut]
   [clojure.java.io :as io]
   [clojure.test :as t]))

(def temp-files
  (repeatedly 2 #(io/file (str "." (gensym)))))

(defn- create-temp-file
  []
  (dotimes [n (count temp-files)]
    (spit (nth temp-files n) (str "before" n))))

(defn- delete-temp-file
  []
  (doseq [^java.io.File f temp-files]
    (when (.exists f)
      (.delete f))))

(defn temp-file-fixture
  [f]
  (create-temp-file)
  (f)
  (delete-temp-file))

(t/use-fixtures :each temp-file-fixture)

(defmethod sut/upgrader ::test
  [dep]
  (str "after" (:latest-version dep)))

(t/deftest upgrade!-test
  (let [[temp1 temp2] temp-files
        dep1 (r/map->Dependency {:project ::test
                                 :latest-version "LATEST"
                                 :file temp1})
        ;; should be skipped because latest-version is nil
        dep2 (r/map->Dependency {:project ::test
                                 :latest-version nil
                                 :file temp2})]
    (t/is (every? #(= 0 (.indexOf % "before"))
                  (map slurp temp-files)))

    (sut/upgrade! [dep1 dep2] true)

    (t/is (= "afterLATEST" (slurp temp1)))
    (t/is (= "before1" (slurp temp2)))))

(t/deftest upgrade!-unsupported-test
  (let [[temp1] temp-files
        dep (r/map->Dependency {:project ::unknown
                                :latest-version "LATEST"
                                :file temp1})]
    (t/is (= "before0" (slurp temp1)))

    (let [out-str (with-out-str (sut/upgrade! [dep] true))]
      (t/is (not= -1 (.indexOf out-str "Not supported"))))

    (t/is (= "before0" (slurp temp1)))))

(t/deftest upgrade!-confirm-test
  (let [[temp1] temp-files
        dep (r/map->Dependency {:project ::test
                                :latest-version "LATEST"
                                :file temp1})]
    (t/is (= "before0" (slurp temp1)))

    (t/testing "input no"
      (let [out-str (with-out-str
                      (with-redefs [read (constantly 'n)]
                        (sut/upgrade! [dep] false)))]
        (t/is (not= -1 (.indexOf out-str "Do you upgrade"))))

      (t/is (= "before0" (slurp temp1))))

    (t/testing "input yes"
      (let [out-str (with-out-str
                      (with-redefs [read (constantly 'y)]
                        (sut/upgrade! [dep] false)))]
        (t/is (not= -1 (.indexOf out-str "Do you upgrade"))))

      (t/is (= "afterLATEST" (slurp temp1))))))
