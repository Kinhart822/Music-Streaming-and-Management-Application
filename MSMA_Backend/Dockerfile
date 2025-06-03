# Build stage
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
VOLUME /tmp
EXPOSE 8080
ARG JAR_FILE=target/MSMA-0.0.1-SNAPSHOT.jar
COPY --from=build /app/${JAR_FILE} app.jar
COPY .env ./
ENTRYPOINT ["java", "-jar", "/app.jar"]