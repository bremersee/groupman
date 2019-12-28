#!/bin/sh
if [ -z "$CONFIG_PASSWORD" ] && [ ! -z "$CONFIG_PASSWORD_FILE" ] && [ -e $CONFIG_PASSWORD_FILE ]; then
  export CONFIG_PASSWORD="$(cat $CONFIG_PASSWORD_FILE)"
fi
java -Djava.security.egd=file:/dev/./urandom -jar /opt/app.jar
