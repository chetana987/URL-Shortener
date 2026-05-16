FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

RUN apk add --no-cache tzdata curl && \
    rm -rf /var/cache/apk/*

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -sf http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["java", \
    "-jar", "app.jar", \
    "-Djava.security.egd=file:/dev/./urandom"]
