/*******************************************************************************
 * Copyright (c) 2021, 2026 Red Hat Inc. and others.
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
 *     Hannes Wellmann - Build Equinox native launchers and executables on demand as part of master- and verification-builds
  *******************************************************************************/

def runOnNativeBuildAgent(String platform, Closure body) {
	def final nativeBuildStageName = 'Perform native launcher build'
	// Use x86 agent for aarch64 build to ensure that the baseline macOS SDK (the oldest) is used consistently for all binary builds
	if (platform == 'cocoa.macosx.aarch64') {
		platform = 'cocoa.macosx.x86_64'
	}
	def agentLabel = 'native.builder-' + platform
	if (platform == 'gtk.linux.x86_64') {
		podTemplate(inheritFrom: 'basic' /* inherit general configuration */, containers: [
			containerTemplate(name: 'launcherbuild', image: 'eclipse/platformreleng-debian-swtgtk3nativebuild:10',
				resourceRequestCpu:'1000m', resourceRequestMemory:'512Mi',
				resourceLimitCpu:'2000m', resourceLimitMemory:'4096Mi',
				alwaysPullImage: true, command: 'cat', ttyEnabled: true)
		]) {
			node(POD_LABEL) { stage(nativeBuildStageName) { container('launcherbuild') { body() } } }
		}
	} else {
		// See the Definition of the RelEng Jenkins instance in
		// https://github.com/eclipse-cbi/jiro/tree/master/instances/eclipse.platform.releng
		node(agentLabel) { stage(nativeBuildStageName) { body() } }
	}
}

def isOnMainIshBranch() {
	return env.BRANCH_NAME == ('master') || env.BRANCH_NAME ==~ 'R[0-9]+_[0-9]+_maintenance'
}

def getLatestLauncherTag() {
	return sh(script: 'git describe --abbrev=0 --tags --match LBv[0-9]*-[0-9][0-9][0-9][0-9]*', returnStdout: true).trim()
}

def buildCurrentLauncherTag() {
	return 'LBv' + getLauncherVersion('maj_ver') + '-' + getLauncherVersion('min_ver')
}

LAUNCHER_SOURCES_PATH = 'features/org.eclipse.equinox.executable.feature/library'

def getLauncherVersion(String segmentName) {
	return sh(script: "grep '${segmentName}=' ${WORKSPACE}/equinox/${LAUNCHER_SOURCES_PATH}/make_version.mak |cut -d= -f2", returnStdout: true).trim()
}

def isLauncherSourceChanged(String baselineTag, String part) {
	return !sh(script: "git diff ${baselineTag} HEAD -- ${LAUNCHER_SOURCES_PATH}/${part}", returnStdout: true).trim().isEmpty()
}

def BUILD_NATIVES = []

pipeline {
	options {
		skipDefaultCheckout() // Specialized checkout is performed below
		timestamps()
		timeout(time: 180, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label 'ubuntu-latest'
	}
	parameters {
		booleanParam(name: 'forceNativeBuilds-cocoa', defaultValue: false, description: 'Enforce a re-build of Equinox\' launcher binaries for Mac OS X. Will push the built binaries to the master branch, unless \'skipCommit\' is set.')
		booleanParam(name: 'forceNativeBuilds-gtk', defaultValue: false, description: 'Enforce a re-build of Equinox\' launcher binaries for Linux. Will push the built binaries to the master branch, unless \'skipCommit\' is set.')
		booleanParam(name: 'forceNativeBuilds-win32', defaultValue: false, description: 'Enforce a re-build of Equinox\' launcher binaries for Windows. Will push the built binaries to the master branch, unless \'skipCommit\' is set.')
		booleanParam(name: 'skipCommit', defaultValue: false, description: 'Stops committing to equinox and equinox binaries repo at the end. Useful in debugging.')
	}
	stages {
		stage('Checkout SCM') {
			steps {
				dir('equinox') {
					checkout scm
					sh '''
							git config --global user.email 'equinox-bot@eclipse.org'
							git config --global user.name 'Eclipse Equinox Bot'
							git remote set-url --push origin git@github.com:eclipse-equinox/equinox.git
							git fetch --all --tags --quiet
					'''
				}
				dir('equinox.binaries') {
					checkout([$class: 'GitSCM',
						branches: [[name: "*/${isOnMainIshBranch() ? env.BRANCH_NAME : 'master'}"]],
						doGenerateSubmoduleConfigurations: false,
						extensions: [[$class: 'CloneOption', timeout: 120]],
						userRemoteConfigs: [[url: 'https://github.com/eclipse-equinox/equinox.binaries.git']]
					])
					sh 'git remote set-url --push origin git@github.com:eclipse-equinox/equinox.binaries.git'
				}
			}
		}
		stage('Check if native launchers and executables changed') {
			steps {
				dir('equinox') {
					script {
						def latestTag = getLatestLauncherTag()
						echo "Latest launcher tag: ${latestTag}"
						def allWS = [ 'cocoa', 'gtk', 'win32' ]
						boolean commonNativesChanged = isLauncherSourceChanged(latestTag, ' ' + allWS.collect{ ws -> "':(exclude)${LAUNCHER_SOURCES_PATH}/${ws}'"}.join(' '))
						if (commonNativesChanged) {
							BUILD_NATIVES += allWS
						} else {
							for (ws in allWS) {
								if (params['forceNativeBuilds-' + ws] || isLauncherSourceChanged(latestTag, ws)) {
									BUILD_NATIVES += ws
								}
							}
						}
						echo "Natives changed since tag '${latestTag}': ${BUILD_NATIVES}, commons changed: ${commonNativesChanged}"
						if (BUILD_NATIVES) {
							// Increment versions if any OS changed and write new tags for changed OS
							dir("${LAUNCHER_SOURCES_PATH}") {
								def newVersion = getLauncherVersion('min_ver').toInteger() + 1
								sh "sed -i -e 's/min_ver=.*/min_ver=${newVersion}/' make_version.mak"
								sh 'cp -r ../tools/* .'
								for (ws in BUILD_NATIVES) {
									stash name:"equinox.launcher.sources.${ws}", includes: "*,${ws}/"
								}
							}
							def newLauncherTag = buildCurrentLauncherTag()
							for (ws in BUILD_NATIVES) {
								sh "sed -i -e 's/binaryTag=.*/binaryTag=${newLauncherTag}/' bundles/org.eclipse.equinox.launcher.${ws}.*/build.properties"
							}
						}
					}
				}
			}
		}
		stage('Build launcher and executable native binaries') {
			when {
				expression { BUILD_NATIVES }
			}
			matrix {
				axes {
					axis {
						name 'PLATFORM'
						values \
							'cocoa.macosx.aarch64' ,\
							'cocoa.macosx.x86_64' ,\
							'gtk.linux.aarch64' ,\
							'gtk.linux.ppc64le' ,\
							'gtk.linux.riscv64',\
							'gtk.linux.x86_64' ,\
							'win32.win32.aarch64' ,\
							'win32.win32.x86_64'
					}
				}
				stages {
					stage('Prepare and post-process native build') {
						options {
							timeout(time: 120, unit: 'MINUTES') // Some build agents are rare and it might take awhile until they are available.
						}
						steps {
							script {
								def (ws, os, arch) = env.PLATFORM.split('\\.')
								if (!(ws in BUILD_NATIVES)) {
									echo "Skip native build for platform ${PLATFORM}"
									return;
								}
								runOnNativeBuildAgent("${PLATFORM}") {
									cleanWs() // workspace not cleaned by default
									echo "Build launcher binaries for OS: ${os}, ARCH: ${arch}"
									unstash "equinox.launcher.sources.${ws}"
									def javaHome = tool(type:'jdk', name: 'temurin-jdk21-latest-auto')
									def extendedPath = "${javaHome}${isUnix() ? '/' : '\\'}bin${isUnix() ? ':' : ';'}${env.PATH}"
									withEnv(["JAVA_HOME=${javaHome}", "PATH=${extendedPath}", "EXE_OUTPUT_DIR=${WORKSPACE}/libs", "LIB_OUTPUT_DIR=${WORKSPACE}/libs"]) {
										if (isUnix()) {
											sh """
												java --version
												java build.java -ws ${ws} -os ${os} -arch ${arch} ${ os == 'linux' ? 'checklibs' : '' } install
											"""
										} else {
											bat """
												java --version
												java build.java -ws ${ws} -os ${os} -arch ${arch} install
											"""
										}
									}
									dir('libs') {
										stash "equinox.binaries.${PLATFORM}"
									}
									cleanWs() // workspace not cleaned by default
								}
								dir("libs/${PLATFORM}") {
									unstash "equinox.binaries.${PLATFORM}"
									withEnv(["ws=${ws}", "os=${os}", "arch=${arch}", "isMainIshBranch=${isOnMainIshBranch()}"]) {
										sh '''
											fnSignFile()
											{
												filename=$1
												signerUrl=$2
												extraArguments=$3
												if [[ ${isMainIshBranch} == true ]]; then
													mv ${filename} unsigned-${filename}
													curl --fail --form "file=@unsigned-${filename}" --output "${filename}" ${extraArguments} "${signerUrl}"
													rm unsigned-${filename}
												fi
											}
											
											binPath=${WORKSPACE}/equinox.binaries/org.eclipse.equinox.executable/bin/${ws}/${os}/${arch}
											libPath=${WORKSPACE}/equinox.binaries/org.eclipse.equinox.launcher.${ws}.${os}.${arch}
											
											if [[ ${PLATFORM} == cocoa.macosx.* ]]; then
												binPath=${binPath}/Eclipse.app/Contents/MacOS
												libFileExt='so'
												# Sign MacOS launcher executable and library
												signerUrl='https://cbi.eclipse.org/macos/codesign/sign'
												fnSignFile eclipse_*.so "${signerUrl}"
												curl --output 'sdk.entitlement' --fail 'https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/refs/heads/master/products/eclipse.entitlement'
												fnSignFile eclipse "${signerUrl}" '--form entitlements=@sdk.entitlement'
											
											elif [[ ${PLATFORM} == gtk.linux.* ]]; then
												libFileExt='so'
											
											elif [[ ${PLATFORM} == win32.win32.* ]]; then
												libFileExt='dll'
												exeFileExt='*.exe'
												# Sign Windoes launcher executables and library
												signerUrl='https://cbi.eclipse.org/authenticode/sign'
												fnSignFile eclipse_*.dll "${signerUrl}"
												fnSignFile eclipse.exe "${signerUrl}"
												fnSignFile eclipsec.exe "${signerUrl}"
											fi
											
											mkdir -p ${binPath}
											mkdir -p ${libPath}
											
											echo 'Clean existing binaries'
											rm -f ${binPath}/eclipse${exeFileExt}
											rm -f ${libPath}/eclipse_*.${libFileExt}
											
											echo 'Copy new binaries'
											mv eclipse${exeFileExt} ${binPath}
											mv eclipse_*.${libFileExt} ${libPath}
											
											echo 'Set file permissions for launcher library'
											chmod 755 ${libPath}/eclipse_*.${libFileExt}
										'''
									}
								}
							}
						}
					}
				}
			}
		}
		stage('Commit built native binaries') {
			when {
				expression { BUILD_NATIVES }
			}
			steps {
				withEnv(["launcherTag=${buildCurrentLauncherTag()}"]) {
					sh '''
						echo "launcherTag: ${launcherTag}"
						pushd equinox
						git add --all
						git status
						git commit -m "Binaries ${launcherTag}"
						git tag ${launcherTag}
						popd
						
						pushd equinox.binaries
						git add --all
						git status
						git commit -m 'Recompiled binaries'
						git tag ${launcherTag}
						popd
					'''
				}
			}
		}
		stage('Build') {
			tools {
				maven 'apache-maven-latest'
				jdk 'temurin-jdk25-latest'
			}
			environment {
				EQUINOX_BINARIES_LOC = "$WORKSPACE/equinox.binaries"
			}
			steps {
				dir('equinox') {
					sh '''
						mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
							-Pbree-libs -Papi-check -Pjavadoc\
							-Dequinox.binaries.loc=$WORKSPACE/equinox.binaries
					'''
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '**/*.log, equinox/**/target/*.jar, equinox.binaries/**', allowEmptyArchive: true
					discoverGitReferenceBuild referenceJob: 'equinox/master'
					junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
					recordIssues publishAllIssues: true, ignoreQualityGate: true, enabledForFailure: true, tools: [
							eclipse(name: 'Compiler', pattern: '**/target/compilelogs/*.xml'),
							issues(name: 'API Tools', id: 'apitools', pattern: '**/target/apianalysis/*.xml'),
							mavenConsole(),
							javaDoc()
						], qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]
				}
			}
		}
		stage('Push built native binaries') {
			when {
				expression { BUILD_NATIVES }
			}
			steps {
				sshagent(['github-bot-ssh']) {
					script {
						def launcherTag = null
						dir('equinox') { // the following command must run within this directory
							launcherTag = getLatestLauncherTag()
						}
						sh """
							echo 'new launcher tag: ${launcherTag}'
							# Check for the main-branch as late as possible to have as much of the same behavior as possible
							if [[ ${isOnMainIshBranch()} == true ]]; then
								if [[ ${params.skipCommit} != true ]]; then
									
									# Don't rebase and just fail in case another commit has been pushed to the master/maintenance branch in the meantime
									pushd equinox
									git push origin HEAD:refs/heads/${BRANCH_NAME}
									git push origin refs/tags/${launcherTag}
									popd
									
									pushd equinox.binaries
									git push origin HEAD:refs/heads/${BRANCH_NAME}
									git push origin refs/tags/${launcherTag}
									popd
									
									exit 0
								else
									echo 'Committing is skipped'
								fi
							else
								echo "Skip pushing changes of native binaries for branch '${BRANCH_NAME}'"
							fi
						"""
					}
				}
			}
		}
	}
}
