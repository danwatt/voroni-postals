FROM maven:3.6.3-jdk-8 AS build
ENV MAVEN_TARGET "clean verify"
CMD ["sh","-c","mvn ${MAVEN_TARGET}"]

FROM openjdk:8-jre-slim as run
COPY target/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","on.port=8080"]