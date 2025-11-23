# Build stage with Maven included
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

# Set encoding environment variables
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8


WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/target/protoBOB-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]