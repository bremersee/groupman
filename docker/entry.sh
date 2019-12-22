#!/bin/sh
CONFIG_PASSWORD="$(cat /run/secrets/config-server-client-user-password)"
java -Djava.security.egd=file:/dev/./urandom -jar /opt/app.jar
