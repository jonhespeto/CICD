#!groovy
// Check properties
properties([disableConcurrentBuilds()])

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        timestamps()
    }
    stages {
        stage('First step') {
            steps {
                sh 'hostname'
            }
        }
        stage('Second step') {
            steps {
                sh 'uptime'
            }
        }
    }
}
