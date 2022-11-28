def call(dockerRepoName, serviceName) {
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
					dir("3850_assignment") {
						sh "docker-compose down || true"
						sh "docker-compose up -d"
					}
				}
			}
			stage('Deploy Stats') {
				steps {
					dir("${serviceName}") {
						STATUS_REPORT = sh (
							script: 'docker inspect --format="Name: {.Name} || Image: {{.Config.Image}} || ContainerIP: {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}} Status:{{.State.Status}}"  $(docker ps -aq -f "name=3850_assignment_")',
							returnStdout: true
						).trim()
						echo "${IP_REPORT}"
						USAGE_REPORT = sh (
							script: 'docker stats --no-stream  $(docker ps -aq -f "name=3850_assignment_")',
							returnStdout: true
						).trim()
						echo "${USAGE_REPORT}"
						echo ""
						sh 'docker inspect --format="Name: {.Name} || Image: {{.Config.Image}} || ContainerIP: {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}} Status:{{.State.Status}}"  $(docker ps -aq -f "name=3850_assignment_") > ip_addr_report.json'
						sh "docker stats --no-stream  $(docker ps -aq -f 'name=3850_assignment_') > usage_report.json"
					// def json = readJSON file: 'deploy_stat_report.json'
					// def jsonFormat = JsonOutput.toJson(json)
					// prettyJSON = JsonOutput.prettyPrint(jsonFormat)
					// echo "${prettyJSON}"
					}
				}
			}
			stage('Zip Artifacts'){
				steps {
					dir("${serviceName}") {
						sh "zip -r ${serviceName}_app.zip *.py"
						archiveArtifacts artifacts: "${serviceName}_app.zip"
					}
				}
			}
	    
		}
	    
	}

}


