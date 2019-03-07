pipeline {
  agent none
  environment {
    SERVICE_NAME='groupman'
    DOCKER_IMAGE='bremersee/groupman'
    DEV_TAG='latest'
    PROD_TAG='release'
    SPRING_PROFILES='default,mongodb,ldap'
    REPLICAS=1
    CONSTRAINT="'node.role == worker'"
  }
  stages {
    stage('Deploy on dev-swarm') {
      agent {
        label 'dev-swarm'
      }
      when {
        branch 'develop'
      }
      steps {
        sh '''
          if docker service ls | grep -q ${SERVICE_NAME}; then
            echo "Updating service ${SERVICE_NAME} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${DEV_TAG} ${SERVICE_NAME}
          else
            echo "Creating service ${SERVICE_NAME} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker service create \
              --replicas ${REPLICAS} \
              --name ${SERVICE_NAME} \
              --hostname ${SERVICE_NAME} \
              --network proxy \
              --label com.df.notify=true \
              --label com.df.servicePath=${SERVICE_NAME} \
              --label com.df.port=80 \
              --label com.df.reqPathSearchReplace="/${SERVICE_NAME}/,/" \
              --secret config-server-client-user-password \
              --restart-delay 10s \
              --restart-max-attempts 10 \
              --restart-window 60s \
              --update-delay 10s \
              --constraint ${CONSTRAINT} \
              -e APPLICATION_NAME="${SERVICE_NAME}" \
              -e ACTIVE_PROFILES="${SPRING_PROFILES}" \
              -e CONFIG_CLIENT_ENABLED="true" \
              -e CONFIG_URI="http://config-server:8888" \
              -e CONFIG_USER="configclient" \
              -e CONFIG_PASSWORD='{{"{{DOCKER-SECRET:config-server-client-user-password}}"}}' \
              -e CONFIG_CLIENT_FAIL_FAST="true" \
              -e CONFIG_RETRY_INIT_INTERVAL="3000" \
              -e CONFIG_RETRY_MAX_INTERVAL="4000" \
              -e CONFIG_RETRY_MAX_ATTEMPTS="8" \
              -e CONFIG_RETRY_MULTIPLIER="1.1" \
              -e SERVER_PORT="80" \
              ${DOCKER_IMAGE}:${DEV_TAG}
          fi
        '''
      }
    }
  }
}