FROM openjdk:11
LABEL maintainer="smartparking.technologies@gmail.com"

WORKDIR /usr/local/
COPY spt-root-0.0.1-SNAPSHOT-standalone.jar /usr/local/
ADD plugins /usr/local/plugins
COPY prod.yml /usr/local/
ENV JAVA_TOOL_OPTIONS "-Dsun.management.jmxremote.level=FINEST \
 -Dsun.management.jmxremote.handlers=java.util.logging.ConsoleHandler \
 -Dcom.sun.management.jmxremote.local.only=false \
 -Dcom.sun.management.jmxremote.ssl=false \
 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.port=5000 \
 -Dcom.sun.management.jmxremote.rmi.port=5000 \
 -Dcom.sun.management.jmxremote.host=0.0.0.0 \
 -Djava.rmi.server.hostname=0.0.0.0"
CMD java -jar -Xmx4g -Xms1g spt-root-0.0.1-SNAPSHOT-standalone.jar --spring.config.location=/usr/local/prod.yml

EXPOSE 8080 5000