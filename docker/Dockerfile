FROM openjdk:11
LABEL maintainer="smartparking.technologies@gmail.com"

WORKDIR /usr/local/
COPY spt-root-0.0.1-SNAPSHOT-standalone.jar /usr/local/
ADD plugins /usr/local/plugins
COPY test.yml /usr/local/
CMD java -jar spt-root-0.0.1-SNAPSHOT-standalone.jar --spring.config.location=/usr/local/test.yml

EXPOSE 8080