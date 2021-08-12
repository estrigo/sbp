FROM openjdk:11
LABEL maintainer="kairzhan79@gmail.com"

WORKDIR /usr/local/
COPY spt-root-0.0.1-SNAPSHOT-standalone.jar /usr/local/
ADD plugins /usr/local/plugins
COPY test.yml /usr/local/
CMD java -jar -Dspring.config.location=/usr/local/test.yml spt-root-0.0.1-SNAPSHOT-standalone.jar

EXPOSE 8080
