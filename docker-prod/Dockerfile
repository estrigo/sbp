FROM openjdk:11
LABEL maintainer="smartparking.technologies@gmail.com"

WORKDIR /usr/local/
COPY spt-root-0.0.1-SNAPSHOT-standalone.jar /usr/local/
ADD plugins /usr/local/plugins
COPY prod.yml /usr/local/
CMD java -jar -Xmx2048m -Xms1024m spt-root-0.0.1-SNAPSHOT-standalone.jar --spring.config.location=/usr/local/prod.yml

EXPOSE 8080