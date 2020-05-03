.PHONY: repl run test

repl:
	iced repl -A:dev

run:
	clojure -m antq.core

test:
	clojure -R:dev -A:test

pom.xml: deps.edn
	clojure -Spom

target/antq.jar: pom.xml
	clojure -A:uberjar

uberjar: target/antq.jar

clean:
	rm -rf target
