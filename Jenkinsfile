pipeline {
  agent none
  environment {
    SERVICE_NAME='groupman'
    DOCKER_IMAGE='bremersee/groupman'
    DEV_TAG='latest'
    PROD_TAG='release'
  }
  stages {
    stage('Test') {
      agent {
        label 'maven'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B clean test'
      }
    }
    stage('Push latest') {
      agent {
        label 'maven'
      }
      when {
        branch 'develop'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=latest package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Push release') {
      agent {
        label 'maven'
      }
      when {
        branch 'master'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=latest package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=release package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
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
            docker-swarm/service.sh "${DOCKER_IMAGE}:${DEV_TAG}" "default,ldap,mongodb,dev"
          fi
        '''
      }
    }
    stage('Deploy snapshot site') {
      agent {
        label 'maven'
      }
      when {
        branch 'develop'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'mvn -B site-deploy'
      }
    }
    stage('Deploy release site') {
      agent {
        label 'maven'
      }
      when {
        branch 'master'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -P gh-pages-site site site:stage scm-publish:publish-scm'
      }
    }
  }
}