pipeline {
  agent none
  environment {
    SERVICE_NAME='groupman'
    DOCKER_IMAGE='bremersee/groupman'
    DEV_TAG='snapshot'
    PROD_TAG='latest'
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
      when {
        not {
          branch 'feature/*'
        }
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B clean test'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
    stage('Push snapshot') {
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
          mvn -B -DskipTests -Ddockerfile.skip=false clean package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=snapshot clean package dockerfile:push
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
          mvn -B -DskipTests -Ddockerfile.skip=false clean package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=latest clean package dockerfile:push
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
      environment {
        CODECOV_TOKEN = credentials('groupman-codecov-token')
      }
      when {
        branch 'develop'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'mvn -B clean site-deploy'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Deploy release site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('groupman-codecov-token')
      }
      when {
        branch 'master'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -P gh-pages-site clean site site:stage scm-publish:publish-scm'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Test feature') {
      agent {
        label 'maven'
      }
      when {
        branch 'feature/*'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B -P feature,allow-features clean test'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
  }
}