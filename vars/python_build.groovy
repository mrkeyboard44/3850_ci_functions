def call(dockerRepoName, serviceName, portNum) {
	pipeline {
	    agent any
		parameters {
			booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
		}
	    stages {
			stage('Build') {
					steps {
						dir("${serviceName}") {
							sh "pwd"
							sh 'pip install -r requirements.txt'
						}
				}
				
			}
			stage('Python Lint') {
				steps {
					dir("${serviceName}") {
						sh 'pylint-fail-under --fail_under 5.0 *.py'
					}
				}

			}
			stage('Package') {
				when {
					expression { env.GIT_BRANCH == 'origin/main' }
				}
				steps {
					withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
						dir("${serviceName}") {
							sh "docker login -u 'mrkeyboard' -p '$TOKEN' docker.io"
							sh "docker build -t ${dockerRepoName}:latest --tag mrkeyboard/${dockerRepoName}:${serviceName} ."
							sh "docker push mrkeyboard/${dockerRepoName}:${serviceName}"
						}
					}
				}
			}
			stage('Deploy') {
				when {
					expression { params.DEPLOY }
				}
				steps {
					sh "docker stop ${dockerRepoName} || true && docker rm ${dockerRepoName} || true"
					sh "docker run -d -p ${portNum}:${portNum} --name ${dockerRepoName} ${dockerRepoName}:latest"
				}
			}

			stage('Zip Artifacts'){
				steps {
					sh 'zip -r app.zip *.py'
					archiveArtifacts artifacts: 'app.zip'
				}
			}
	    
		}
	    
	}

}


