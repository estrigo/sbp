FROM openjdk:11
LABEL maintainer="smartparking.technologies@gmail.com"

WORKDIR /usr/local/
COPY spt-root-0.0.1-SNAPSHOT-standalone.jar /usr/local/
ADD plugins /usr/local/plugins
COPY prod.yml /usr/local/
ENV JAVA_TOOL_OPTIONS "-Dcom.sun.management.jmxremote.ssl=false \
 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.port=5000 \
 -Dcom.sun.management.jmxremote.rmi.port=5000 \
 -Dcom.sun.management.jmxremote.host=0.0.0.0 \
 -Djava.rmi.server.hostname=0.0.0.0"
CMD java -jar -Xmx2048m -Xms1024m spt-root-0.0.1-SNAPSHOT-standalone.jar --spring.config.location=/usr/local/prod.yml

EXPOSE 8080 5000