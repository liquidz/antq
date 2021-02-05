(ns antq.log-test
  (:require
   [antq.log :as sut]
   [clojure.test :as t]))

(t/deftest info-test
  (t/is (= "INFO\n"
           (with-out-str (sut/info "INFO")))))

(t/deftest error-test
  (let [sw (java.io.StringWriter.)
        err-str (binding [*err* sw]
                  (sut/error "ERROR")
                  (str sw))]
    (t/is (= "ERROR\n" err-str))))
