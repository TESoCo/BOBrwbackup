# ==================== SPRING BOOT BUILDER ====================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS springboot-builder

WORKDIR /app/springboot

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ==================== FASTAPI BUILDER ====================
FROM python:3.11-slim AS fastapi-builder

WORKDIR /app/fastapi

# Crear y activar entorno virtual
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Instalar dependencias en el entorno virtual
RUN pip install --no-cache-dir \
    fastapi \
    uvicorn \
    python-multipart \
    pymongo \
    pillow \
    reportlab \
    python-dotenv \
    requests

COPY main.py .

# ==================== RUNTIME FINAL ====================
FROM eclipse-temurin:17-jre-alpine

# Instalar Python (sin pip3)
RUN apk add --no-cache python3

# Crear entorno virtual en el runtime
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Copiar las dependencias desde el builder
COPY --from=fastapi-builder /opt/venv /opt/venv

# Copiar el código de FastAPI
COPY --from=fastapi-builder /app/fastapi/main.py ./fastapi/

WORKDIR /app

# Copiar Spring Boot JAR
COPY --from=springboot-builder /app/springboot/target/protoCOB-*.jar springboot-app.jar

ENV SPRINGBOOT_URL=http://localhost:8080

EXPOSE 8080 8000

# Script de inicio
RUN echo '#!/bin/sh\n\
echo "🚀 Iniciando Spring Boot..."\n\
java -jar /app/springboot-app.jar &\n\
\n\
sleep 5\n\
\n\
echo "🚀 Iniciando FastAPI..."\n\
cd /app/fastapi\n\
/opt/venv/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8000\n\
' > /app/start.sh && chmod +x /app/start.sh

CMD ["/app/start.sh"]