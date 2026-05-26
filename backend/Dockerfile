# ─────────────────────────────────────────────────────
# STAGE 1 — build
# Compila el proyecto con Gradle y genera el JAR
# ─────────────────────────────────────────────────────
FROM gradle:8-jdk21 AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Descarga dependencias primero (aprovecha caché de Docker)
RUN gradle dependencies --no-daemon || true

COPY src ./src

RUN gradle bootJar --no-daemon -x test

# ─────────────────────────────────────────────────────
# STAGE 2 — runtime
# Imagen mínima Alpine con solo el JRE — ~200 MB final
# ─────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S upsglam && adduser -S upsglam -G upsglam
USER upsglam

# Copia solo el JAR compilado desde stage 1
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
