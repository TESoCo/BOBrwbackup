# Build stage with Maven included
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Set encoding environment variables
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

# Copy project files
COPY pom.xml .
COPY src ./src
#COPY .env .env

# Build the application
RUN mvn clean package -DskipTests



# DEBUG: List all files to see what was created
RUN echo "=== Listing all files in target directory ==="
RUN ls -la /app/target/
RUN echo "=== Listing JAR files ==="
RUN ls -la /app/target/*.jar || echo "No JAR files found"
RUN echo "=== Checking file sizes ==="
RUN find /app/target/ -name "*.jar" -exec echo "{}: " \; -exec du -h {} \;



# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /

# Copy the built JAR
COPY --from=builder /app/target/protoBOB-0.0.1-SNAPSHOT.jar app.jar

# Verify the JAR was copied successfully
RUN echo "=== Verifying JAR in runtime image ===" && \
    ls -la /app.jar && \
    echo "JAR file exists: $(test -f /app.jar && echo 'YES' || echo 'NO')"


# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]