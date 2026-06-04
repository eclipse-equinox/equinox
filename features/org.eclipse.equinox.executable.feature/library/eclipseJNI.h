/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at 
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
#ifndef ECLIPSE_JNI_H
#define ECLIPSE_JNI_H

#include "eclipseUnicode.h"
#include <jni.h>

typedef struct {
	int launchResult;
	int runResult;
	_TCHAR * errorMessage;
} JavaResults;

typedef jint (JNICALL *JNI_createJavaVM)(JavaVM **pvm, JNIEnv **env, void *args);

/* JNI Callback methods */
/* Use name mangling since we may be linking these from java with System.LoadLibrary */
#define set_exit_data 		Java_org_eclipse_equinox_launcher_JNIBridge__1set_1exit_1data
#define set_launcher_info	Java_org_eclipse_equinox_launcher_JNIBridge__1set_1launcher_1info
#define get_os_recommended_folder 	    Java_org_eclipse_equinox_launcher_JNIBridge__1get_1os_1recommended_1folder

#ifdef __cplusplus
extern "C" {
#endif
/*
 * org_eclipse_equinox_launcher_JNIBridge#_set_exit_data
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL set_exit_data(JNIEnv *, jobject, jstring, jstring);

/*
 * org_eclipse_equinox_launcher_JNIBridge#_set_launcher_info
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL set_launcher_info(JNIEnv *, jobject, jstring, jstring);

/*
 * org_eclipse_equinox_launcher_JNIBridge#_get_os_recommended_folder
 * Signature: ()Ljava/lang/String
 */
JNIEXPORT jstring JNICALL get_os_recommended_folder(JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif

/* Start the Java VM and Wait For It to Terminate
 *
 * This method is responsible for starting the Java VM and for
 * detecting its termination. The resulting JVM exit code should
 * be returned to the main launcher, which will display a message if
 * the termination was not normal.
 */
extern JavaResults* startJavaJNI( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile );

extern void cleanupVM( int );
#endif
