# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY registry-service/pom.xml registry-service/pom.xml
COPY session-orchestrator/pom.xml session-orchestrator/pom.xml
COPY signaling-service/pom.xml signaling-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
RUN mvn -q -B -pl common,gateway-service -am dependency:go-offline || true

COPY common common
COPY gateway-service gateway-service
RUN mvn -q -B -pl gateway-service -am clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r cloudplay && useradd -r -g cloudplay cloudplay
COPY --from=build /workspace/gateway-service/target/gateway-service.jar app.jar
USER cloudplay
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
