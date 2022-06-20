(ns antq.util.async-test
  (:require
   [antq.util.async :as sut]
   [clojure.core.async :as async]
   [clojure.test :as t]))

(def ^:private test-async-fn
  (sut/fn-with-timeout
   (fn [x]
     (cond
       (= ::error x)
       (throw (Exception. "test error"))

       (= ::timeout x)
       (do (async/<!! (async/timeout 500))
           x)

       :else
       x))
   100))

(t/deftest fn-with-timeout-test
  (t/is (= 10 (test-async-fn 10)))
  (t/is (nil? (test-async-fn ::timeout)))
  (t/is (thrown-with-msg? Exception #"^test error$"
          (test-async-fn ::error))))
