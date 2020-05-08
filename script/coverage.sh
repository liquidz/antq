#!/bin/bash

clojure -A:dev:coverage --src-ns-path=src --test-ns-path=test --codecov
