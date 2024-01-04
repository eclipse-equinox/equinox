pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
			    sh 'git submodule update --init --recursive --remote'
				sh """
				mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
					-Pbree-libs -Papi-check -Pjavadoc\
					-Dcompare-version-with-baselines.skip=false \
					-Dproject.build.sourceEncoding=UTF-8 \
				"""
			}
			post {
				always {
					archiveArtifacts artifacts: '**/*.log, **/*.jar', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'equinox/master'
					recordIssues publishAllIssues: true, tools: [java(), mavenConsole(), javaDoc()]
				}
			}
		}
	}
}
