/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     LinuxPasswordProviderMessages.java
 *******************************************************************************/
#include <jni.h>
#include "keystoreLinuxNative.h"
#include <libsecret/secret.h>
#include <sys/types.h>
#include <stdlib.h>
#include <string.h>

const SecretSchema *
equinox_get_schema (void)
{
    static const SecretSchema the_schema = {
        "org.eclipse.equinox", SECRET_SCHEMA_NONE,
        {
            {  NULL, 0 },
        }
    };
    return &the_schema;
}

#define EQUINOX_SCHEMA  equinox_get_schema ()

static void unlock_secret_service(JNIEnv *env)
{
	GError *error = NULL;
	GList *l, *ul;
	gchar* lbl;
	gint nu;
	//check that there is session dbus bus
	GDBusConnection* dbusconnection = g_bus_get_sync(G_BUS_TYPE_SESSION, NULL, &error);
	if (error != NULL) {
		(*env)->ExceptionClear(env);
		g_prefix_error(&error, "Unable to get DBus session bus: ");
	 	(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), error->message);
	 	g_error_free (error);
	 	return;
	}
	SecretService*	secretservice = secret_service_get_sync(SECRET_SERVICE_LOAD_COLLECTIONS, NULL, &error);
 	if (error != NULL) {
	  (*env)->ExceptionClear(env);
 	  	g_prefix_error(&error, "Unable to get secret service: ");
 		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), error->message);
 		g_error_free (error);
 		return;
 	}

	SecretCollection*	defaultcollection = secret_collection_for_alias_sync(secretservice, SECRET_COLLECTION_DEFAULT, SECRET_COLLECTION_NONE, NULL, &error);
 	if (error != NULL) {
	  (*env)->ExceptionClear(env);
 	  	g_prefix_error(&error, "Unable to get secret collection: ");
 		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), error->message);
 		g_error_free (error);
 		return;
 	}
 	if (defaultcollection == NULL) {
 		(*env)->ExceptionClear(env);
 		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), "Unable to find default secret collection");
 		return;
 	}

	if (secret_collection_get_locked(defaultcollection))
	{
		lbl = secret_collection_get_label(defaultcollection);
		l = NULL;
		l = g_list_append(l, defaultcollection);
		nu = secret_service_unlock_sync(secretservice, l, NULL, &ul, &error);
		g_list_free(l);
		g_list_free(ul);
		if (error != NULL) {
		  (*env)->ExceptionClear(env);
	 	  	g_prefix_error(&error, "Unable to unlock: ");
	 		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), error->message);
	 		g_error_free (error);
	 		return;
	 	}

	}
	return;
}

JNIEXPORT jstring JNICALL Java_org_eclipse_equinox_internal_security_linux_LinuxPasswordProvider_getMasterPassword(JNIEnv *env, jobject this) {
  GError *error = NULL;
  jstring result;

  unlock_secret_service(env);
  if ((*env)->ExceptionOccurred(env)) {
    return NULL;
  }

	gchar *password = secret_password_lookup_sync(EQUINOX_SCHEMA, NULL, &error, NULL);

	if (error != NULL) {
	    (*env)->ExceptionClear(env);
		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), error->message);
	    g_error_free (error);
	    return NULL;
	} else if (password == NULL) {
	    (*env)->ExceptionClear(env);
		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), "Unable to find password");
		return NULL;
	} else {
	    result = (*env)->NewStringUTF(env, password);
		free(password);
	    return result;
	}
}

JNIEXPORT void JNICALL Java_org_eclipse_equinox_internal_security_linux_LinuxPasswordProvider_saveMasterPassword(JNIEnv *env, jobject this, jstring password) {
	GError *error = NULL;

	unlock_secret_service(env);
	if ((*env)->ExceptionOccurred(env)) {
		return;
	}

	const char *passwordUTF = (*env)->GetStringUTFChars(env, password, NULL);
	secret_password_store_sync (EQUINOX_SCHEMA, SECRET_COLLECTION_DEFAULT,
			"Equinox master password", passwordUTF, NULL, &error,
			NULL);

	// free the UTF strings
	(*env)->ReleaseStringUTFChars( env, password, passwordUTF );

	if (error != NULL) {
		(*env)->ExceptionClear(env);
		(*env)->ThrowNew(env, (* env)->FindClass(env, "java/lang/SecurityException"), error->message);
		g_error_free (error);
	}
}

