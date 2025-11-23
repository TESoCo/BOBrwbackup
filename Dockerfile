# Build stage
FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .


# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Render will assign one)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]