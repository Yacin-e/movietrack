# Stage 1: build (context = repo root)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY proto/ proto/
COPY tracking-service/.mvn/ tracking-service/.mvn/
COPY tracking-service/mvnw tracking-service/pom.xml tracking-service/
WORKDIR /workspace/tracking-service
RUN ./mvnw dependency:go-offline -q
COPY tracking-service/src/ src/
RUN ./mvnw clean package -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/tracking-service/target/tracking-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
