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
		stage('get binaries') {
			steps{
				dir ('rt.equinox.binaries') {
					checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', timeout: 120]], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/eclipse-equinox/equinox.binaries.git']]])
				}
			}
		}
		stage('Build') {
			steps {
				sh """
				mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
					-Pbree-libs -Papi-check -Pjavadoc\
					-Dcompare-version-with-baselines.skip=false \
					-Dproject.build.sourceEncoding=UTF-8 \
					-Drt.equinox.binaries.loc=$WORKSPACE/rt.equinox.binaries 
				"""
			}
			post {
				always {
					archiveArtifacts artifacts: '**/*.log, **/*.jar', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'equinox/master'
					recordIssues publishAllIssues: true, tools: [eclipse(pattern: '**/target/compilelogs/*.xml'), mavenConsole(), javaDoc()]
				}
			}
		}
	}
}
