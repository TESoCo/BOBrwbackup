# ==================== SPRING BOOT BUILDER ====================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS springboot-builder

WORKDIR /app/springboot

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ==================== RUNTIME ====================
FROM python:3.11-slim

WORKDIR /app

# Instalar dependencias de Python
RUN pip install --no-cache-dir \
    fastapi \
    uvicorn \
    python-multipart \
    pymongo \
    pillow \
    reportlab \
    python-dotenv \
    requests

# Instalar Java
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    rm -rf /var/lib/apt/lists/*

# Copiar Spring Boot
COPY --from=springboot-builder /app/springboot/target/protoCOB-*.jar springboot-app.jar

# Copiar FastAPI
COPY main.py ./fastapi/

ENV SPRINGBOOT_URL=http://localhost:8080

EXPOSE 8080 8000

# Script de inicio que corre ambos servicios
RUN echo '#!/bin/bash\n\
echo "🚀 Iniciando Spring Boot..."\n\
java -jar /app/springboot-app.jar &\n\
\n\
# Esperar a que Spring Boot arranque\n\
sleep 10\n\
\n\
echo "🚀 Iniciando FastAPI..."\n\
cd /app/fastapi\n\
python3 -m uvicorn main:app --host 0.0.0.0 --port 8000\n\
' > /app/start.sh && chmod +x /app/start.sh

CMD ["/app/start.sh"]