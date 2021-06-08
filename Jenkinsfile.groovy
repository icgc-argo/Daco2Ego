/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of
 * the GNU Affero General Public License v3.0. You should have received a copy of the
 * GNU Affero General Public License along with this program.
 *  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

def commit = "UNKNOWN"
def serviceName = "daco2ego"
def repoName = "icgc-argo"
def dockerRepo = "ghcr.io/${repoName}/${serviceName}"

pipeline {
  agent {
    kubernetes {
        label 'daco2ego-executor'
        yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
    volumeMounts:
    - name: docker-graph-storage
      mountPath: /var/lib/docker
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
"""
    }
  }
  stages {
    stage('Prepare') {
      steps {
        script {
            commit = sh(returnStdout: true, script: 'git describe --always').trim()
        }
      }
    }
    stage('Publish Docker') {
      when {
          branch "master"
      }
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
          }
          sh "docker  build --build-arg COMMIT_ID=${commit} --network=host -f Dockerfile . -t ${dockerRepo}:latest -t ${dockerRepo}:${commit}"
          sh "docker push ${dockerRepo}:${commit}"
          sh "docker push ${dockerRepo}:latest"
        }
      }
    }
  }
}