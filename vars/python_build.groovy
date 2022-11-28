def call(dockerRepoName, imageName, portNum) {
	pipeline {
	    agent any
		parameters {
			booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
		}
	    stages {
			stage('Build') {
					steps {
					sh 'pip install -r requirements.txt'
				}
				
			}
			stage('Python Lint') {
				steps {
					sh 'pylint-fail-under --fail_under 5.0 *.py'
				}

			}
			stage('Test and Coverage') {
				steps {
					script {
						scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
						echo "$scriptDir"
						sh 'rm -rfv *test-reports/*'
						def files = findFiles(glob: "**/test_*.py")
						for (file in files) {
							def file_path = file.path
							sh "coverage run --omit */site-packages/*,*/dist-packages/* $file_path"
						}
						sh 'coverage report'
					}
				}
				post {
					always {
						script {
							def files = findFiles(glob: "**/*test-reports/*.xml")
							for (file in files) {
								def file_path = file.path
								junit "$file_path"
							}
						}

					}
				}

			}
			stage('Package') {
				when {
					expression { env.GIT_BRANCH == 'origin/main' }
				}
				steps {
					withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
						sh "docker login -u 'mrkeyboard' -p '$TOKEN' docker.io"
						sh "docker build -t ${dockerRepoName}:latest --tag mrkeyboard/${dockerRepoName}:${imageName} ."
						sh "docker push mrkeyboard/${dockerRepoName}:${imageName}"
					}
				}
			}
			stage('Deliver') {
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


