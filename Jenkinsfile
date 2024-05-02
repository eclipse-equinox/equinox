/*******************************************************************************
 * Copyright (c) 2021, 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
  *******************************************************************************/

def isOnMainIshBranch() {
	return env.BRANCH_NAME == ('master') || env.BRANCH_NAME ==~ 'R[0-9]+_[0-9]+_maintenance'
}

pipeline {
	options {
		skipDefaultCheckout() // Specialiced checkout is performed below
		timestamps()
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
	environment {
		EQUINOX_BINARIES_LOC = "$WORKSPACE/equinox.binaries"
	}
	stages {
		stage('Checkout SCM') {
			steps{
				dir('equinox') {
					checkout scm
				}
				dir('equinox.binaries') {
					checkout([$class: 'GitSCM',
						branches: [[name: "*/${isOnMainIshBranch() ? env.BRANCH_NAME : 'master'}"]],
						doGenerateSubmoduleConfigurations: false,
						extensions: [[$class: 'CloneOption', timeout: 120]],
						userRemoteConfigs: [[url: 'https://github.com/eclipse-equinox/equinox.binaries.git']]
					])
				}
			}
		}
		stage('Build') {
			steps {
				dir('equinox') {
					sh '''
						mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
							-Pbree-libs -Papi-check -Pjavadoc\
							-Drt.equinox.binaries.loc=$WORKSPACE/equinox.binaries
					'''
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '**/*.log, **/*.jar', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'equinox/master'
					recordIssues publishAllIssues: true, tools: [eclipse(name: 'Compiler and API Tools', pattern: '**/target/compilelogs/*.xml'), mavenConsole(), javaDoc()]
				}
			}
		}
	}
}
