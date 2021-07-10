ARTIFACT=target/antq.jar

.PHONY: repl
repl:
	iced repl -A:dev

.PHONY: outdated
outdated:
	clojure -M:outdated:nop --upgrade

.PHONY: test
test: install
	# NOTE: kaocha does not support Clojure 1.8
	clojure -M:outdated:nop:1.8 --exclude=clojure/brew-install --exclude=org.clojure/tools.deps.alpha --exclude=com.github.seancorfield/depstar --exclude=lambdaisland/kaocha
	clojure -M:dev:1.9:test
	clojure -M:dev:test
	script/integration_test.sh

.PHONY: lint
lint:
	cljstyle check
	clj-kondo --lint src:test

.PHONY: pom
pom:
	clojure -Spom

target/antq-standalone.jar: pom
	clojure -X:depstar uberjar :jar $@ :aot true :main-class antq.core :aliases '[:nop]'

.PHONY: uberjar
uberjar: clean target/antq-standalone.jar

$(ARTIFACT): pom
	clojure -X:depstar jar :jar $@
.PHONY: jar
jar: clean $(ARTIFACT)

.PHONY: install
install: clean $(ARTIFACT)
	clojure -X:deploy :installer :local :artifact $(ARTIFACT)

.PHONY: deploy
deploy: clean $(ARTIFACT)
	echo "Testing if CLOJARS_USERNAME environmental variable exists."
	test $(CLOJARS_USERNAME)
	clojure -X:deploy :installer :remote :artifact $(ARTIFACT)

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
