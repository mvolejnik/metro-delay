FROM azul/zulu-openjdk-alpine:23.0.1-23.30-jre-headless

RUN apk -U add perf 
ADD metrodelay-ws-rest-uber.jar /

RUN addgroup -g 1000 java 
RUN adduser -D -h /home/java -s /bin/sh -G java -u 1000 java

USER java
WORKDIR /

ENTRYPOINT ["java", "-jar", "metrodelay-ws-rest-uber.jar"]
