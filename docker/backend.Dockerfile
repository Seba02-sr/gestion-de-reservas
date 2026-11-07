# syntax=docker/dockerfile:1.7
#
# Dockerfile multi-etapa del backend (un solo archivo para los 5 servicios Java).
# Contexto de build: la RAÍZ del repositorio (docker compose lo configura: context: ..).
#
#   etapa "build"   -> Maven + JDK 21: compila el reactor completo UNA sola vez (los 5 targets
#                      reutilizan esta misma etapa) y deja los jars en /src/**/target.
#                      Las dependencias de Maven se cachean entre builds (BuildKit cache mount).
#   etapa "runtime" -> JRE 21 liviano, usuario no-root; una etapa final por servicio (targets):
#                      user-svc | gestion-svc | reservas-svc | gateway | eureka
#
# Uso (lo hace docker/docker-compose.yml):  docker build -f docker/backend.Dockerfile --target gateway .

# ---------------------------------------------------------------- build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Poms primero: si solo cambia código fuente, esta capa y la resolución de dependencias se reutilizan.
COPY pom.xml ./
COPY common/dan-common-lib/pom.xml common/dan-common-lib/
COPY common/dan-eureka-server/pom.xml common/dan-eureka-server/
COPY common/dan-spring-gateway/pom.xml common/dan-spring-gateway/
COPY services/user-svc/pom.xml services/user-svc/
COPY services/gestion-svc/pom.xml services/gestion-svc/
COPY services/reservas-svc/pom.xml services/reservas-svc/

# Código fuente de todos los módulos
COPY common common
COPY services services

# -Dmaven.test.skip=true: la imagen no ejecuta tests (se corren en CI y con ./mvnw install);
# algunos usan Testcontainers y necesitan un Docker que no existe dentro del build.
RUN --mount=type=cache,id=m2-reservas,target=/root/.m2 \
    mvn -B -ntp -q -Dmaven.test.skip=true package

# ---------------------------------------------------------------- base de ejecución
FROM eclipse-temurin:21-jre-alpine AS runtime
# Usuario sin privilegios (uid fijo 10001)
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app -h /app app
WORKDIR /app
USER app

# ---------------------------------------------------------------- servicios (targets)
FROM runtime AS user-svc
# El actuator escucha en un puerto de gestión aparte (9091): no se publica ni lo enruta el gateway
ENV MANAGEMENT_SERVER_PORT=9091
COPY --from=build --chown=app:app /src/services/user-svc/target/user-svc-*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:9091/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS gestion-svc
# El actuator escucha en un puerto de gestión aparte (9091): no se publica ni lo enruta el gateway
ENV MANAGEMENT_SERVER_PORT=9091
COPY --from=build --chown=app:app /src/services/gestion-svc/target/gestion-svc-*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:9091/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS reservas-svc
# El actuator escucha en un puerto de gestión aparte (9091): no se publica ni lo enruta el gateway
ENV MANAGEMENT_SERVER_PORT=9091
COPY --from=build --chown=app:app /src/services/reservas-svc/target/reservas-svc-*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:9091/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS gateway
# El actuator escucha en un puerto de gestión aparte (9091): no se publica ni lo enruta el gateway
ENV MANAGEMENT_SERVER_PORT=9091
COPY --from=build --chown=app:app /src/common/dan-spring-gateway/target/dan-spring-gateway-*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:9091/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS eureka
COPY --from=build --chown=app:app /src/common/dan-eureka-server/target/dan-eureka-server-*.jar /app/app.jar
EXPOSE 8761
# Eureka no usa puerto de gestión aparte: su actuator está en el 8761
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8761/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
