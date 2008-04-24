/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#include <jni.h>
#include "keystoreNative.h"
#include <CoreFoundation/CoreFoundation.h>
#include <Security/Security.h>
#include <CoreServices/CoreServices.h>

/**
 * Implements the get password functionality.
 */ 
jstring getPassword(JNIEnv *env, jobject this, jstring serviceName, jstring accountName) {
	OSStatus status;
	UInt32 passwordLength = (UInt32) nil;
	void *passwordData = nil;
	const char *serviceNameUTF = (*env)->GetStringUTFChars(env, serviceName, NULL);
	const char *accountNameUTF = (*env)->GetStringUTFChars(env, accountName, NULL);

	status = SecKeychainFindGenericPassword (NULL,
			(*env)->GetStringUTFLength(env, serviceName), serviceNameUTF,
			(*env)->GetStringUTFLength(env, accountName), accountNameUTF,
			&passwordLength, &passwordData,
			NULL
	);
	
	// free the UTF strings
	(*env)->ReleaseStringUTFChars( env, serviceName, serviceNameUTF );
	(*env)->ReleaseStringUTFChars( env, accountName, accountNameUTF );

	// throw an exception if we have an error
	if (status != noErr) {
		(*env)->ExceptionClear(env);
		char buffer [60];
		sprintf(buffer, "Could not obtain password.  Result: %d", (int) status);
		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), buffer);
	}

	// massage the string into a Java-friendly UTF-8 string
	char *truncatedPassword = (char *) malloc(passwordLength * sizeof(char) + 1);
	strncpy(truncatedPassword, passwordData, passwordLength * sizeof(char));
	truncatedPassword[passwordLength * sizeof(char)] = '\0';
	jstring result = (*env)->NewStringUTF(env, truncatedPassword);
	free(truncatedPassword);
	// free the returned password data
	SecKeychainItemFreeContent (NULL, passwordData);
	return result;
}

JNIEXPORT jstring JNICALL Java_org_eclipse_equinox_internal_security_osx_OSXProvider_getPassword(JNIEnv *env, jobject this, jstring serviceName, jstring accountName) {
	return getPassword(env, this, serviceName, accountName);
}

/**
 * Implements the set password functionality.
 */ 
void setPassword(JNIEnv *env, jobject this, jstring serviceName, jstring accountName, jstring password) {
	OSStatus status;
	const char *serviceNameUTF = (*env)->GetStringUTFChars(env, serviceName, NULL);
	const char *accountNameUTF = (*env)->GetStringUTFChars(env, accountName, NULL);
	const char *passwordUTF = (*env)->GetStringUTFChars(env, password, NULL);
	
	// attempt to add the password
	status = SecKeychainAddGenericPassword (NULL,
		(*env)->GetStringUTFLength(env, serviceName), serviceNameUTF,
		(*env)->GetStringUTFLength(env, accountName), accountNameUTF,
		(*env)->GetStringUTFLength(env, password), passwordUTF, 
		NULL);

	// it already exists, try to change it
	if (status == errSecDuplicateItem) {
		SecKeychainItemRef itemRef = (SecKeychainItemRef) nil;
	    
		// find the ItemRef corresponding to the item
		status = SecKeychainFindGenericPassword (NULL,
		(*env)->GetStringUTFLength(env, serviceName), serviceNameUTF,
		(*env)->GetStringUTFLength(env, accountName), accountNameUTF,
		NULL, NULL,
		&itemRef
		);
		
		// this should rarely happen - we're in this state because it exists.
		if (status != noErr) {
			// free the UTF strings
			(*env)->ReleaseStringUTFChars( env, serviceName, serviceNameUTF );
			(*env)->ReleaseStringUTFChars( env, accountName, accountNameUTF );
			(*env)->ReleaseStringUTFChars( env, password, passwordUTF );
			// release the pointer to the item
			// the following code craps out for some unknown reason when called from within Eclipse.  It works fine in a standalone app, however.
			//if (itemRef) CFRelease(itemRef);

			(*env)->ExceptionClear(env);
			char buffer [60];
			sprintf(buffer, "Could not obtain password.  Result: %d", (int) status);
			(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), buffer);
		}

		// modify the data based on the item we've gotten above
		status = SecKeychainItemModifyAttributesAndData (itemRef, NULL, (*env)->GetStringUTFLength(env, password), passwordUTF);

		// free the UTF strings
		(*env)->ReleaseStringUTFChars( env, serviceName, serviceNameUTF );
		(*env)->ReleaseStringUTFChars( env, accountName, accountNameUTF );
		(*env)->ReleaseStringUTFChars( env, password, passwordUTF );
		
		// release the pointer to the item
		// the following code craps out for some unknown reason when called from within Eclipse.  It works fine in a standalone app, however.
		//if (itemRef) CFRelease(itemRef);

		// throw an exception if it didnt work
		if (status != noErr) {
			(*env)->ExceptionClear(env);
			char buffer [60];
			sprintf(buffer, "Could change password.  Result: %d", (int) status);
			(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), buffer);
		}

	}
}

JNIEXPORT void JNICALL Java_org_eclipse_equinox_internal_security_osx_OSXProvider_setPassword(JNIEnv *env, jobject this, jstring serviceName, jstring accountName, jstring password) {
	setPassword(env, this, serviceName, accountName, password);
}

