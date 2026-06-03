# syntax=docker/dockerfile:1.7

# ---- build: compile + package the Spring Boot fat jar on a JDK 17 image ----
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Resolve dependencies in a cached layer separate from the source so code-only
# changes don't re-download the world. Normalise line endings because Windows
# clones can hand us a CRLF mvnw that the shell refuses to exec.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests clean package \
    && mv target/wotos-user-service-*.jar target/app.jar

# ---- runtime: ship the jar on a slim JRE 17 image as a non-root user ----
FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system app && useradd --system --gid app --create-home --home-dir /home/app app
USER app
WORKDIR /home/app

COPY --from=builder /workspace/target/app.jar /home/app/app.jar

# Configured service port (server.port=4646 in wotos-config).
EXPOSE 4646
ENTRYPOINT ["java","-jar","/home/app/app.jar"]
