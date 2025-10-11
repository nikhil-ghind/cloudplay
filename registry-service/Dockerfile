# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy the reactor poms first to leverage Docker layer caching for dependencies.
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY registry-service/pom.xml registry-service/pom.xml
COPY session-orchestrator/pom.xml session-orchestrator/pom.xml
COPY signaling-service/pom.xml signaling-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
RUN mvn -q -B -pl common,registry-service -am dependency:go-offline || true

# Copy sources and build just this service (+ its module deps).
COPY common common
COPY registry-service registry-service
RUN mvn -q -B -pl registry-service -am clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r cloudplay && useradd -r -g cloudplay cloudplay
COPY --from=build /workspace/registry-service/target/registry-service.jar app.jar
USER cloudplay
EXPOSE 8083
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
