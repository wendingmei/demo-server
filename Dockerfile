FROM java:8

EXPOSE 8080

ADD ./demo-server.jar demo-server.jar

ENTRYPOINT ["java", "-jar", "/demo-server","-Xms512m -Xmx512m -XX:+UseConcMarkSweepGC"]