# =============================================================================
# Nombre: Dockerfile (backend)
# Descripción: Imagen multi-etapa del Back-End SIGIF (Maven + JRE 21).
# Fecha de creación: 21/10/2026
# Última actualización: 21/10/2026
# Autor: Equipo SIGIF
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sigif && adduser -S sigif -G sigif
COPY --from=build /app/target/sigif-backend-*.jar app.jar
USER sigif
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
