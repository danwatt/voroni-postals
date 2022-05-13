FROM maven:3.8.5-openjdk-18-slim AS build
ENV MAVEN_TARGET "clean verify"
CMD ["sh","-c","mvn ${MAVEN_TARGET}"]

FROM openjdk:18-alpine as run
COPY target/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","on.port=8080"]