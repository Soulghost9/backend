# Multi-stage Docker build for SDRP Backend
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/sdrp-backend.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
