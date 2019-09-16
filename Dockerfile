FROM openjdk:11.0.4-jre-slim
MAINTAINER Murilo Pereira <murilo@murilopereira.com>

ADD target/uberjar/search-engine-indexer-0.2.0-SNAPSHOT-standalone.jar /srv

ENTRYPOINT ["java", "-jar", "/srv/search-engine-indexer-0.2.0-SNAPSHOT-standalone.jar"]
