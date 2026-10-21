# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appbancaria && adduser -S appbancaria -G appbancaria
COPY --from=build /app/target/appbancaria-api-0.0.1-SNAPSHOT.jar app.jar
USER appbancaria
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
