FROM openjdk:11-jdk-slim
MAINTAINER Christian Bremer <bremersee@googlemail.com>
ARG JAR_FILE
ADD target/${JAR_FILE} /opt/app.jar
ADD docker/entrypoint.sh /opt/entrypoint.sh
RUN chmod 755 /opt/entrypoint.sh
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.cloud.config.password=$(cat /run/secrets/config-server-client-user-password)", "-jar", "/opt/app.jar"]
