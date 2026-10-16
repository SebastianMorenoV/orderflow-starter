# ==========================================
# Etapa 1: Build y empaquetado con Maven y JDK 21
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copiar descriptores de dependencias primero para optimizar el cache de capas
COPY orderflow-starter/pom.xml .
COPY orderflow-starter/orders-api/pom.xml orders-api/
COPY orderflow-starter/notifications-lambda/pom.xml notifications-lambda/

# Descargar dependencias en caché
RUN mvn dependency:go-offline -B || true

# Copiar código fuente de los módulos
COPY orderflow-starter/orders-api/src orders-api/src
COPY orderflow-starter/notifications-lambda/src notifications-lambda/src

# Compilar y generar artefacto empaquetado (fat jar de Spring Boot)
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Runtime ligero con JRE 21 (Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario y grupo sin privilegios (Principio de menor privilegio)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el artefacto ejecutable generado en la etapa de compilación
COPY --from=builder --chown=appuser:appgroup /build/orders-api/target/orders-api-*.jar app.jar

# Cambiar a usuario no root
USER appuser

# Exponer el puerto de la aplicación (Spring Boot)
EXPOSE 8080

# Health check usando Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Comando de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]
