pipeline {
  agent none
  environment {
    SERVICE_NAME='groupman'
    DOCKER_IMAGE='bremersee/groupman'
    DEV_TAG='latest'
    PROD_TAG='release'
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
            chmod 755 docker-swarm/service.sh
            docker-swarm/service.sh "${DOCKER_IMAGE}:${DEV_TAG}"
          fi
        '''
      }
    }
  }
}