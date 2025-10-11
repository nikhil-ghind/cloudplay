# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY registry-service/pom.xml registry-service/pom.xml
COPY session-orchestrator/pom.xml session-orchestrator/pom.xml
COPY signaling-service/pom.xml signaling-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
RUN mvn -q -B -pl common,session-orchestrator -am dependency:go-offline || true

COPY common common
COPY session-orchestrator session-orchestrator
RUN mvn -q -B -pl session-orchestrator -am clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r cloudplay && useradd -r -g cloudplay cloudplay
COPY --from=build /workspace/session-orchestrator/target/session-orchestrator.jar app.jar
USER cloudplay
EXPOSE 8082
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
