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

# Copiar el código de FastAPI
COPY main.py .

# ==================== RUNTIME FINAL ====================
FROM eclipse-temurin:17-jre-alpine

# Instalar Python y supervisor (sin pip3)
RUN apk add --no-cache python3 supervisor

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

# Crear archivo de configuración de supervisor
RUN echo '[supervisord]\n\
nodaemon=true\n\
logfile=/var/log/supervisord.log\n\
pidfile=/var/run/supervisord.pid\n\
\n\
[program:springboot]\n\
command=java -jar /app/springboot-app.jar\n\
autostart=true\n\
autorestart=true\n\
stdout_logfile=/var/log/springboot.log\n\
stderr_logfile=/var/log/springboot-error.log\n\
\n\
[program:fastapi]\n\
command=/opt/venv/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8000\n\
directory=/app/fastapi\n\
autostart=true\n\
autorestart=true\n\
stdout_logfile=/var/log/fastapi.log\n\
stderr_logfile=/var/log/fastapi-error.log\n\
' > /etc/supervisord.conf

# IMPORTANTE: Ambos servicios están en el mismo contenedor
ENV SPRINGBOOT_URL=http://localhost:8080
ENV PATH="/opt/venv/bin:$PATH"

# Exponer ambos puertos
EXPOSE 8080 8000

# Iniciar supervisor
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]