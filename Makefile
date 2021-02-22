.PHONY: repl outdated test pom jar uberjar install deploy docker coverage clean

ARTIFACT=target/antq.jar

repl:
	iced repl -A:dev

outdated:
	clojure -M:outdated:nop --upgrade

test:
	# NOTE: kaocha does not support Clojure 1.8
	clojure -M:outdated:nop:1.8
	clojure -M:dev:1.9:test
	clojure -M:dev:test

lint:
	cljstyle check
	clj-kondo --lint src:test

pom:
	clojure -Spom

target/antq-standalone.jar: pom
	clojure -A:nop -X:depstar uberjar :jar $@ :aot true :main-class antq.core

uberjar: clean target/antq-standalone.jar

$(ARTIFACT): pom
	clojure -X:depstar jar :jar $@
jar: clean $(ARTIFACT)

install: clean $(ARTIFACT)
	clojure -X:deploy :installer :local :artifact $(ARTIFACT)

deploy: clean $(ARTIFACT)
	echo "Testing if CLOJARS_USERNAME environmental variable exists."
	test $(CLOJARS_USERNAME)
	clojure -X:deploy :installer :remote :artifact $(ARTIFACT)

docker:
	docker build -t uochan/antq .
docker-test:
	docker run --rm -v $(shell pwd):/src -w /src uochan/antq:latest

coverage:
	clojure -M:coverage:dev:nop --src-ns-path=src --test-ns-path=test --codecov

clean:
	rm -rf .cpcache target
