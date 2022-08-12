.PHONY: outdated
outdated:
	clojure -M:outdated:nop --upgrade

.PHONY: test
test: install
	clojure -M:dev:1.10:test
	clojure -M:dev:test
	script/integration_test.sh

.PHONY: lint
lint:
	cljstyle check
	clj-kondo --lint src:test

.PHONY: uberjar
uberjar: clean
	clojure -T:build uberjar

.PHONY: jar
jar: clean
	clojure -T:build jar

.PHONY: install
install: clean
	clojure -T:build install

.PHONY: docker
docker:
	docker build -t uochan/antq .
.PHONY: docker-test
docker-test:
	docker run --rm -v $(shell pwd):/src -w /src uochan/antq:latest

.PHONY: coverage
coverage:
	clojure -M:coverage:dev:nop --src-ns-path=src --test-ns-path=test --codecov

.PHONY: clean
clean:
	rm -rf .cpcache target
