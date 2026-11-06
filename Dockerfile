FROM eclipse-temurin:25-jre-noble
LABEL org.opencontainers.image.authors="christian@bremersee.org"

EXPOSE 8080

ENV ACTIVE_PROFILES=in-memory

ENV CONFIG_CLIENT_ENABLED=false
ENV CONFIG_URI=https://config.eixe.bremersee.org
ENV CONFIG_USER=config
ENV CONFIG_PASSWORD=changeit
ENV CONFIG_LABEL=develop
ENV SERVER_PORT=8080

RUN mkdir -p /app

COPY target/groupman.jar /app/app.jar

WORKDIR /app

ENTRYPOINT [ \
  "java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${ACTIVE_PROFILES}", \
  "-jar", \
  "app.jar" \
]
