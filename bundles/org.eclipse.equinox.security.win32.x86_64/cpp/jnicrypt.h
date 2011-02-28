/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
#include <jni.h>

#ifndef EQUINOX_WIN32_CRYPTO
#define EQUINOX_WIN32_CRYPTO

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL Java_org_eclipse_equinox_internal_security_win32_WinCrypto_windecrypt(JNIEnv *, jobject, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_org_eclipse_equinox_internal_security_win32_WinCrypto_winencrypt(JNIEnv *, jobject, jbyteArray);

#ifdef __cplusplus
}
#endif

#endif // #ifndef EQUINOX_WIN32_CRYPTO

