pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
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
					-Pbuild-individual-bundles -Pbree-libs -Papi-check \
					-Dcompare-version-with-baselines.skip=false -Dcompare-version-with-baselines.skip=false \
					-Dproject.build.sourceEncoding=UTF-8
					-Drt.equinox.binaries.loc=$WORKSPACE/rt.equinox.binaries 
				"""
			}
			post {
				always {
					archiveArtifacts artifacts: '*.log,*/target/work/data/.metadata/*.log,*/tests/target/work/data/.metadata/*.log,apiAnalyzer-workspace/.metadata/*.log', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
				}
			}
		}
	}
}
