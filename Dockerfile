# Farmácia Clark — API (Spring Boot)
# Build: docker build -t farmacia-api .
# Run:   docker run -p 8080:8080 --env-file .env.prod farmacia-api

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache maven

COPY pom.xml .
COPY farmacia-domain/pom.xml farmacia-domain/
COPY farmacia-application/pom.xml farmacia-application/
COPY farmacia-infrastructure/pom.xml farmacia-infrastructure/
COPY farmacia-api/pom.xml farmacia-api/

COPY farmacia-domain/src farmacia-domain/src
COPY farmacia-application/src farmacia-application/src
COPY farmacia-infrastructure/src farmacia-infrastructure/src
COPY farmacia-api/src farmacia-api/src

RUN mvn -q package -pl farmacia-api -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S farmacia && adduser -S farmacia -G farmacia
USER farmacia

COPY --from=build /workspace/farmacia-api/target/farmacia-api-*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
