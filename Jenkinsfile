def commit = "UNKNOWN" 
def serviceName = "daco2ego"
def dockerHubRepo = "icgcargo"
def gitHubRepo = "icgc-argo"
def dockerRegistry = "ghcr.io"
pipeline {
    agent {
        kubernetes {
            label 'daco2ego-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: docker
    image: docker:18-git
    tty: true
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
      runAsUser: 0
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
"""
        }
    }
    stages {
        stage('Build') {
            steps {
                container('docker') {
                    script {
                        commit = sh(returnStdout: true, script: 'git describe --always').trim()
                    }
                    withCredentials([usernamePassword(credentialsId:'argoDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "docker login -u $USERNAME -p $PASSWORD"
                    }
                    sh "docker build --network=host . -t ${dockerHubRepo}/${serviceName}:${commit}"
                    sh "docker push ${dockerHubRepo}/${serviceName}:${commit}"

                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --network=host . -t ${gitHubRegistry}/${gitHubRepo}/${serviceName}:${commit}"
                    sh "docker push ${gitHubRegistry}/${gitHubRepo}/${serviceName}:${commit}"
                }
            }
        }

        stage('Deploy to argo-dev') {
            when { branch 'develop' }
            steps {
               container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }

                    sh "docker build --network=host -f Dockerfile . -t ${dockerHubRepo}/${serviceName}:edge"
                    sh "docker push ${dockerHubRepo}/${serviceName}:edge"

                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                    }

                    sh "docker build --network=host . -t ${gitHubRegistry}/${gitHubRepo}/${serviceName}:edge"
                    sh "docker push ${gitHubRegistry}/${gitHubRepo}/${serviceName}:edge"

               }
                build(job: "/ARGO/provision/daco2ego", parameters: [
                     [$class: 'StringParameterValue', name: 'AP_ARGO_ENV', value: 'dev' ],
                     [$class: 'StringParameterValue', name: 'AP_ARGS_LINE', value: "--set-string image.tag=${commit}" ]
                ])
            }
        }

        stage('Deploy to argo-qa') {
            when { branch 'master' }
            steps {
               container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }

                    sh "docker build --network=host -f Dockerfile . -t ${dockerHubRepo}/${serviceName}:latest"
                    sh "docker push ${dockerHubRepo}/${serviceName}:latest"

                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                    }

                    sh "docker build --network=host . -t ${gitHubRegistry}/${gitHubRepo}/${serviceName}:latest"
                    sh "docker push ${gitHubRegistry}/${gitHubRepo}/${serviceName}:latest"

                    withCredentials([usernamePassword(credentialsId: 'argoGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${commit}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/icgc-argo/daco2ego --tags"
                    }
                }
                build(job: "/ARGO/provision/daco2ego", parameters: [
                     [$class: 'StringParameterValue', name: 'AP_ARGO_ENV', value: 'qa' ],
                     [$class: 'StringParameterValue', name: 'AP_ARGS_LINE', value: "--set-string image.tag=${commit}" ]
                ])
            }
        }

    }

    post {
        always 
    }
 
}
