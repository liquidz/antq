ARTIFACT=target/antq.jar

.PHONY: repl
repl:
	iced repl -A:dev

.PHONY: outdated
outdated:
	clojure -M:outdated:nop --upgrade

.PHONY: test
test: install
	clojure -M:dev:1.9:test
	clojure -M:dev:test
	script/integration_test.sh

.PHONY: lint
lint:
	cljstyle check
	clj-kondo --lint src:test

.PHONY: uberjar
uberjar:
	clojure -T:build uberjar

.PHONY: jar
jar:
	clojure -T:build jar

.PHONY: install
install:
	clojure -T:build install

.PHONY: deploy
deploy:
	echo "Testing if CLOJARS_USERNAME environmental variable exists."
	test $(CLOJARS_USERNAME)
	clojure -T:build deploy

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
