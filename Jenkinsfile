#!/usr/bin/env groovy

def label = "is-jenkins-${UUID.randomUUID().toString()}"

podTemplate(name: label, label: label, nodeSelector: 'function=docker_workers', containers: [
  containerTemplate(name: 'docker', image: 'docker:stable-dind', ttyEnabled: true, command: 'cat'),
], volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')]) {

  node(label) {
    ansiColor('xterm') {

      stage('Clean Docker Image') {
        container('docker') {
            sh"""
            echo "y"|docker system prune -a
            """
        }
      }
    stage('Clean Docker Image') {
      build job: 'test-param-trigger', parameters: [string(name: 'BRANCH', value: env.BRANCH_NAME), string(name: 'ENVIRONMENT', value: 'STAGE'), string(name: 'SLACK_ROOM', value: 'cx-eng')]
    }
   }
  }
}
