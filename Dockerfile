####
# Multi-stage Dockerfile for Quarkus medsync-backend
# Stage 1: Maven builds the Quarkus app
# Stage 2: UBI OpenJDK runtime serves the app
####

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY .mvn/ ./.mvn/
COPY mvnw ./

RUN mvn -B -q dependency:go-offline

COPY src ./src

RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: Runtime ----
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

ENV LANGUAGE='en_US:en'

COPY --from=build --chown=185 /workspace/target/quarkus-app/lib/      /deployments/lib/
COPY --from=build --chown=185 /workspace/target/quarkus-app/*.jar     /deployments/
COPY --from=build --chown=185 /workspace/target/quarkus-app/app/      /deployments/app/
COPY --from=build --chown=185 /workspace/target/quarkus-app/quarkus/  /deployments/quarkus/

EXPOSE 8080
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
