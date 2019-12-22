FROM openjdk:11-jdk
MAINTAINER Christian Bremer <bremersee@googlemail.com>
ARG JAR_FILE
ADD target/${JAR_FILE} /opt/app.jar
ADD docker/entry.sh /opt/entry.sh
RUN chmod 755 /opt/entry.sh
ENTRYPOINT ["/opt/entry.sh"]
