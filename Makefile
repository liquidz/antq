.PHONY: repl outdated test pom jar uberjar install deploy docker coverage clean

repl:
	iced repl -A:dev

outdated:
	clojure -M:outdated:nop --upgrade

test:
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

target/antq.jar: pom
	clojure -X:depstar jar :jar $@
jar: clean target/antq.jar

install: clean target/antq.jar
	clj -M:deploy install target/antq.jar

deploy: clean target/antq.jar
	echo "Testing if CLOJARS_USERNAME environmental variable exists."
	test $(CLOJARS_USERNAME)
	clj -M:deploy deploy target/antq.jar

docker:
	docker build -t uochan/antq .
docker-test:
	docker run --rm -v $(shell pwd):/src -w /src uochan/antq:latest

coverage:
	clojure -M:coverage:dev:nop --src-ns-path=src --test-ns-path=test --codecov

clean:
	rm -rf .cpcache target
