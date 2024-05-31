#!groovy

properties([disableConcurrentBuilds()])

pipeline {
    agent any
    environment {
        DOCKER_IMAGE = 'web-test-image'
        CONTAINER_NAME = 'web-test-container'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        timestamps()
    }
    stages {
        stage('Checkout code') {
            steps {
                git branch: 'main', url: 'https://github.com/jonhespeto/web.git'
            }
        }
        stage('Modify HTML') {
            steps {
                script {
                    def htmlContent = readFile('index.html')
                    htmlContent = htmlContent.replaceAll('<!-- TITLE -->', web_TITLE)
                                             .replaceAll('<!-- CONTENT_1 -->', web_CONTENT_1)
                                             .replaceAll('<!-- CONTENT_2 -->', web_CONTENT_2)
                                             .replaceAll('<!-- CONTENT_3 -->', web_CONTENT_3)
                                             .replaceAll('<!-- CONTENT_4 -->', web_CONTENT_4)
                                             .replaceAll('<!-- CONTENT_5 -->', web_CONTENT_5)
                                             .replaceAll('<!-- CONTENT_6 -->', web_CONTENT_6)
                                             .replaceAll('<!-- CONTENT_7 -->', web_CONTENT_7)
                    writeFile(file: 'index.html', text: htmlContent)
                }
            }
        }
        stage('Show Modified Page') {
            steps {
                script {
                    def modifiedHtmlContent = readFile('index.html')
                    echo "Modified HTML page content:n${modifiedHtmlContent}"
                }
            }
        }
        stage('Build and Run Docker') {
            steps {
                script {
                    sh 'docker build -t ${DOCKER_IMAGE} .'
                    sh 'docker run -d --name ${CONTAINER_NAME} -p 8888:80 ${DOCKER_IMAGE}'
                    sh 'sleep 5'
                    sh 'curl --fail http://localhost:8888/index.html || exit 1'
                    sh "curl http://localhost:8888 | grep '${web_TITLE}' || exit 1"
                    sh 'curl -s http://localhost:8888 | grep "<img src=" | grep -oP "src="K[^"]+" | xargs -I {} curl --fail -o /dev/null {} || exit 1'
                }
            }
        }
        stage('Replace index.html') {
            when {
                expression { return currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                sshagent(['ssh_key_for_nginx']) {
                    sh "scp -P ${SERVER_PORT} -o StrictHostKeyChecking=no index.html ${SERVER_USER}@${SERVER_HOST}:${SERVER_FROM_DIRECTORY}/"
                    sh "ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'sudo cp ${SERVER_FROM_DIRECTORY}/index.html ${DEPLOY_DIRECTORY}/index.html'"
                    sh "ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'sudo nginx -s reload'"
                }
            }
        }
    }
    post {
        always {
            script {
                sh "docker stop ${CONTAINER_NAME}"
                sh "docker rm ${CONTAINER_NAME}"
                sh "docker rmi ${DOCKER_IMAGE}"
            }
        }
    }
}
