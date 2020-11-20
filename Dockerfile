FROM clojure:openjdk-14-tools-deps

RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /tmp/antq
COPY deps.edn /tmp/antq/deps.edn
COPY src/ /tmp/antq/src/
RUN clojure -Spom && \
        clojure -A:nop -X:depstar uberjar :jar antq.jar :aot true :main-class antq.core

WORKDIR /tmp
ENTRYPOINT ["java", "-jar", "/tmp/antq/antq.jar"]
