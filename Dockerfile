FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Make wrapper executable
RUN chmod +x mvnw
# Download dependencies (resolve instead of go-offline to handle platform-specific artifacts)
RUN ./mvnw dependency:resolve -B -q || true

COPY src ./src
# Build the application
RUN ./mvnw clean package -DskipTests -q

# Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
