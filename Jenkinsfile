pipeline {
    agent any

    environment {
        SBT_FLAGS = "-Dsbt.log.noformat=true"
    }

    stages {
        stage('Cleaning') {
            steps {
                echo 'Cleaning...'
                sh 'sbt ${SBT_FLAGS} clean'
            }
        }
        stage('Formatting') {
            steps {
                echo 'Formatting...'
                sh 'sbt ${SBT_FLAGS} format'
                sh '''
                  echo "Modified files through scalariform:"
                  git diff
                  test -z "$(git ls-files -m)"
                '''
            }
        }
        stage('Building') {
            steps {
                echo 'Building...'
                sh 'sbt ${SBT_FLAGS} compile'
            }
        }
        stage('Testing') {
            steps {
                echo 'Testing...'
                sh 'sbt ${SBT_FLAGS} coverage test coverageReport coverageAggregate'
            }
        }
        stage('Documentation') {
            steps {
                echo 'Testing...'
                sh 'sbt ${SBT_FLAGS} doc unidoc'
            }
        }
        stage('User manual') {
            steps {
                echo 'Building user manual...'
                sh 'sbt ${SBT_FLAGS} evalUserManual'
                sh '''
                  echo "Modified files through evalUserManual:"
                  git diff
                  test -z "$(git ls-files -m)"
                '''

            }
        }
        stage('Packaging') {
            steps {
                echo 'Packaging...'
                sh 'sbt ${SBT_FLAGS} releaseDist'
            }
        }
    }
    post {
        success {
            withCredentials([string(credentialsId: 'codecov-token', variable: 'CODECOV_TOKEN')]) {
                sh '''
                  curl -Os https://uploader.codecov.io/latest/linux/codecov
                  chmod +x codecov
                  ./codecov -t ${CODECOV_TOKEN}
                '''
            }
        }
        fixed {
            emailext (
                subject: "FIXED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: "See <${env.BUILD_URL}>",
                recipientProviders: [developers(), requestor()],
                to: 'gapt-group@googlegroups.com'
            )
        }
        failure {
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: "See <${env.BUILD_URL}>\n\n------------------------------------------\n\${BUILD_LOG}",
                recipientProviders: [developers(), requestor()],
                to: 'gapt-group@googlegroups.com'
            )
        }
  }
}
