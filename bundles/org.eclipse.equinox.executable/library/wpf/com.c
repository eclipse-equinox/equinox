/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/

#include <jni.h>
#include <ole2.h>

#define COM_NATIVE(func) Java_org_eclipse_equinox_launcher_JNIBridge_##func

JNIEXPORT jint JNICALL COM_NATIVE(OleInitialize)
	(JNIEnv *env, jclass that, jint arg0)
{
	return (jint)OleInitialize((LPVOID)arg0);
}

JNIEXPORT void JNICALL COM_NATIVE(OleUninitialize)
	(JNIEnv *env, jclass that)
{
	OleUninitialize();
}

