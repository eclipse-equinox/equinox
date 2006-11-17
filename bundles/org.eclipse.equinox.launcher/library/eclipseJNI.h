/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
#ifndef ECLIPSE_JNI_H
#define ECLIPSE_JNI_H

#include "eclipseUnicode.h"
#include <jni.h>

#ifdef UNICODE
#define loadVMLibrary loadVMLibraryW
#define unloadVMLibrary unloadVMLibraryW
#define getInvocationFunction getInvocationFunctionW
#define launchJavaVM launchJavaVMW
#define startJavaVM startJavaVMW
#endif

#ifdef UNICODE
#define JNI_GetStringChars(env, s) 			 (*env)->GetStringChars(env, s, 0)
#define JNI_ReleaseStringChars(env, s, data) (*env)->ReleaseStringChars(env, s, data)
#else
#define JNI_GetStringChars(env, s) 			 (*env)->GetStringUTFChars(env, s, 0)
#define JNI_ReleaseStringChars(env, s, data) (*env)->ReleaseStringUTFChars(env, s, data)
#endif

typedef jint (JNICALL *JNI_createJavaVM)(JavaVM **pvm, JNIEnv **env, void *args);

/* JNI Callback methods */
/* Use name mangling since we may be linking these from java with System.LoadLibrary */
#define set_exit_data 		Java_org_eclipse_core_launcher_JNIBridge__1set_1exit_1data
#define update_splash 		Java_org_eclipse_core_launcher_JNIBridge__1update_1splash
#define show_splash			Java_org_eclipse_core_launcher_JNIBridge__1show_1splash
#define get_splash_handle 	Java_org_eclipse_core_launcher_JNIBridge__1get_1splash_1handle

#ifdef __cplusplus
extern "C" {
#endif
/*
 * org_eclipse_core_launcher_JNIBridge#_set_exit_data
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL set_exit_data(JNIEnv *, jobject, jstring);

/*
 * org_eclipse_core_launcher_JNIBridge#_update_splash
 * Signature: ()V
 */
JNIEXPORT void JNICALL update_splash(JNIEnv *, jobject);

/*
 * org_eclipse_core_launcher_JNIBridge#_get_splash_handle
 * Signature: ()I
 */
JNIEXPORT jint JNICALL get_splash_handle(JNIEnv *, jobject);

/*
 * org_eclipse_core_launcher_JNIBridge#_show_splash
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL show_splash(JNIEnv *, jobject, jstring);

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
extern int startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[] );
#endif
