#!groovy

properties([disableConcurrentBuilds()])

pipeline {
    agent any
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
        stage('Deploy to server') {
            steps {
                sshagent(['ssh_key_for_nginx']) {
                    sh "scp -r -p ${SERVER_PORT} * ${SERVER_USER}@${SERVER_HOST}:${DEPLOY_DIRECTORY}"
                }
            }
        }
        stage('Restart NGINX') {
            steps {
                sshagent([ssh_key_for_nginx]) {
                    sh "ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'sudo nginx -s reload'"
                }
            }
        }
    }
}
