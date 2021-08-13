(ns antq.tool-test
  (:require
   [antq.tool :as sut]
   [clojure.test :as t]))

(t/deftest prepare-options-test
  (t/testing "initial"
    (t/is (= {:exclude []
              :focus []
              :skip []
              :error-format nil
              :reporter "table"
              :directory ["."]}
             (sut/prepare-options {}))))

  (t/testing "sequential"
    (t/is (= {:exclude []
              :focus []
              :skip []
              :error-format nil
              :reporter "table"
              :directory ["." "foo" "bar"]}
             (sut/prepare-options {:directory ['foo 'bar]}))))

  (t/testing "string"
    (t/is (= {:exclude []
              :focus []
              :skip []
              :error-format nil
              :reporter "foo"
              :directory ["."]}
             (sut/prepare-options {:reporter "foo"}))))

  (t/testing "additional"
    (t/is (= {:exclude []
              :focus []
              :skip []
              :error-format nil
              :reporter "table"
              :directory ["."]
              :upgrade true}
             (sut/prepare-options {:upgrade true})))))
