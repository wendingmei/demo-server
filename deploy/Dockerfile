#FROM registry.cn-hangzhou.aliyuncs.com/hyl-image/java:8
FROM java:8

WORKDIR /deployments

ADD demo-server.jar .

ENTRYPOINT java -jar demo-server.jar $JAVA_OPTIONS