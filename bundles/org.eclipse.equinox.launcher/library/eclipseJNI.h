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
#define setExitData setExitDataW
#endif

typedef jint (JNICALL *JNI_createJavaVM)(JavaVM **pvm, JNIEnv **env, void *args);

void setExitData(JNIEnv *env, jstring s);

/* JNI Callback methods */
/* Use name mangling since we may be linking these from java with System.LoadLibrary */
#define set_exit_data 		Java_org_eclipse_core_launcher_JNIBridge__1set_1exit_1data
#define update_splash 		Java_org_eclipse_core_launcher_JNIBridge__1update_1splash
#define get_splash_handle 	Java_org_eclipse_core_launcher_JNIBridge__1get_1splash_1handle

/*
 * org_eclipse_core_launcher_JNIBridge#_set_exit_data
 * Signature: (Ljava/lang/String;)V
 */
extern JNIEXPORT void JNICALL set_exit_data(JNIEnv *, jobject, jstring);

/*
 * org_eclipse_core_launcher_JNIBridge#_update_splash
 * Signature: ()V
 */
extern JNIEXPORT void JNICALL update_splash(JNIEnv *, jobject);

/*
 * org_eclipse_core_launcher_JNIBridge#_get_splash_handle
 * Signature: ()I
 */
extern JNIEXPORT jint JNICALL get_splash_handle(JNIEnv *, jobject);

#endif
