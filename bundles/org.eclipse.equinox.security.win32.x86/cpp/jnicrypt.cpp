/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers

#include <windows.h>
#include <wincrypt.h>
#include "jnicrypt.h"

BOOL APIENTRY DllMain(HMODULE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved)
{
    return TRUE;
}

JNIEXPORT jbyteArray JNICALL Java_org_eclipse_equinox_internal_security_win32_WinCrypto_winencrypt
  (JNIEnv *env, jobject obj, jbyteArray value)
{
	jsize size = env->GetArrayLength(value);
	jbyte *body = env->GetByteArrayElements(value, NULL);
	if (body == NULL)
		return NULL;

	DATA_BLOB clearText;
	DATA_BLOB encryptedText;
	clearText.pbData = (BYTE*) body;
	clearText.cbData = (DWORD) size;

	BOOL result = CryptProtectData(&clearText, L"Equinox", NULL, NULL, NULL, 0, &encryptedText);

    // release memory allocated by Java environment
	env->ReleaseByteArrayElements(value, body, 0);

    if (result == FALSE)
		return NULL;

    jbyteArray returnArray = env->NewByteArray(encryptedText.cbData);
	env->SetByteArrayRegion(returnArray, 0, encryptedText.cbData, (jbyte*) encryptedText.pbData);
	LocalFree(encryptedText.pbData); // no need any more, have Java representation

	return returnArray;
}

JNIEXPORT jbyteArray JNICALL Java_org_eclipse_equinox_internal_security_win32_WinCrypto_windecrypt
  (JNIEnv *env, jobject obj, jbyteArray value)
{
	jsize size = env->GetArrayLength(value);
	jbyte *body = env->GetByteArrayElements(value, NULL);
	if (body == NULL)
		return NULL;

	DATA_BLOB clearText;
	DATA_BLOB encryptedText;
	encryptedText.pbData = (BYTE*) body;
	encryptedText.cbData = (DWORD) size;

	LPWSTR pDescrOut =  NULL;
	BOOL result = CryptUnprotectData(&encryptedText, &pDescrOut, NULL, NULL, NULL, 0, &clearText);

	if (pDescrOut != NULL)
		LocalFree(pDescrOut);

    // release memory allocated by Java environment
	env->ReleaseByteArrayElements(value, body, 0);

	if (result == FALSE)
		return NULL;

    jbyteArray returnArray = env->NewByteArray(clearText.cbData);
	env->SetByteArrayRegion(returnArray, 0, clearText.cbData, (jbyte*) clearText.pbData);
	LocalFree(clearText.pbData); // no need any more, have Java representation

	return returnArray;
}
