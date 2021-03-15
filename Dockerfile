FROM clojure:openjdk-15-tools-deps-1.10.1.739

RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /tmp/antq
COPY deps.edn /tmp/antq/deps.edn
COPY src/ /tmp/antq/src/
RUN clojure -Spom && \
        clojure -X:depstar uberjar :jar antq.jar :aot true :main-class antq.core :aliases '[:nop]'

WORKDIR /tmp
ENTRYPOINT ["java", "-jar", "/tmp/antq/antq.jar"]
