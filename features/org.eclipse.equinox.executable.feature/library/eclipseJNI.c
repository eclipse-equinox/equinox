/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * 	   Andrew Niefer
 *******************************************************************************/
 
#include "eclipseJNI.h"
#include "eclipseCommon.h"
#include "eclipseOS.h"
#include "eclipseShm.h"

#include <stdlib.h>
#include <string.h>


static _TCHAR* failedToLoadLibrary = _T_ECLIPSE("Failed to load the JNI shared library \"%s\".\n");
static _TCHAR* createVMSymbolNotFound = _T_ECLIPSE("The JVM shared library \"%s\"\ndoes not contain the JNI_CreateJavaVM symbol.\n");
static _TCHAR* failedCreateVM = _T_ECLIPSE("Failed to create the Java Virtual Machine.\n");
static _TCHAR* internalExpectedVMArgs = _T_ECLIPSE("Internal Error, the JVM argument list is empty.\n");
static _TCHAR* mainClassNotFound = _T_ECLIPSE("Failed to find a Main Class in \"%s\".\n");

static JNINativeMethod natives[] = {{"_update_splash", "()V", (void *)&update_splash},
									{"_get_splash_handle", "()J", (void *)&get_splash_handle},
									{"_set_exit_data", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)&set_exit_data},
									{"_set_launcher_info", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)&set_launcher_info},
									{"_show_splash", "(Ljava/lang/String;)V", (void *)&show_splash},
									{"_takedown_splash", "()V", (void *)&takedown_splash}};

/* local methods */
static jstring newJavaString(JNIEnv *env, _TCHAR * str);
static void registerNatives(JNIEnv *env);
static int shouldShutdown(JNIEnv *env);
static void JNI_ReleaseStringChars(JNIEnv *env, jstring s, const _TCHAR* data);
static const _TCHAR* JNI_GetStringChars(JNIEnv *env, jstring str);
static char * getMainClass(JNIEnv *env, _TCHAR * jarFile);
static void setLibraryLocation(JNIEnv *env, jobject obj);

static JavaVM * jvm = 0;
static JNIEnv *env = 0;

/* cache String class and methods to avoid looking them up all the time */
static jclass string_class = NULL;
#if !defined(UNICODE) && !defined(MACOSX)
static jmethodID string_getBytesMethod = NULL;
static jmethodID string_ctor = NULL;
#endif

/* JNI Callback methods */
JNIEXPORT void JNICALL set_exit_data(JNIEnv * env, jobject obj, jstring id, jstring s){
	const _TCHAR* data = NULL;
	const _TCHAR* sharedId = NULL;
	size_t length;
	 
	if(s != NULL) {
		length = (*env)->GetStringLength(env, s);
		if(!(*env)->ExceptionOccurred(env)) {
			data = JNI_GetStringChars(env, s);
			if (data != NULL) {
				if(id != NULL) {
					sharedId = JNI_GetStringChars(env, id);
					if(sharedId != NULL) {
						setSharedData(sharedId, data);
						JNI_ReleaseStringChars(env, id, sharedId);
					}
				} else {
					exitData = malloc((length + 1) * sizeof(_TCHAR*));
					_tcsncpy( exitData, data, length);
					exitData[length] = _T_ECLIPSE('\0');
				}
				JNI_ReleaseStringChars(env, s, data);
			}
		}
		if(data == NULL && sharedId == NULL) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}
}

JNIEXPORT void JNICALL set_launcher_info(JNIEnv * env, jobject obj, jstring launcher, jstring name){
	const _TCHAR* launcherPath = NULL;
	const _TCHAR* launcherName = NULL;
	
	if (launcher != NULL) {
		launcherPath = JNI_GetStringChars(env, launcher);
		if (launcherPath != NULL) {
			setProgramPath(_tcsdup(launcherPath));
			JNI_ReleaseStringChars(env, launcher, launcherPath);
		}
	}
	
	if (name != NULL) {
		launcherName = JNI_GetStringChars(env, name);
		if (launcherName != NULL) {
			setOfficialName(_tcsdup(launcherName));
			JNI_ReleaseStringChars(env, name, launcherName);
		}
	}
}


JNIEXPORT void JNICALL update_splash(JNIEnv * env, jobject obj){
	dispatchMessages();
}

JNIEXPORT jlong JNICALL get_splash_handle(JNIEnv * env, jobject obj){
	return getSplashHandle();
}

JNIEXPORT void JNICALL show_splash(JNIEnv * env, jobject obj, jstring s){
	const _TCHAR* data = NULL;
	
	setLibraryLocation(env, obj);
	
	if(s != NULL) {
		data = JNI_GetStringChars(env, s);
		if(data != NULL) {
			showSplash(data);
			JNI_ReleaseStringChars(env, s, data);
		} else {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}
}

JNIEXPORT void JNICALL takedown_splash(JNIEnv * env, jobject obj){
	takeDownSplash();
}

/*
 * On AIX we need the location of the eclipse shared library so that we
 * can find the libeclipse-motif.so library.  Reach into the JNIBridge
 * object to get the "library" field.
 */
static void setLibraryLocation(JNIEnv * env, jobject obj) {
	jclass bridge = (*env)->FindClass(env, "org/eclipse/equinox/launcher/JNIBridge");
	if (bridge != NULL) {
		jfieldID libraryField = (*env)->GetFieldID(env, bridge, "library", "Ljava/lang/String;");
		if (libraryField != NULL) {
			jstring stringObject = (jstring) (*env)->GetObjectField(env, obj, libraryField);
			if (stringObject != NULL) {
				const _TCHAR * str = JNI_GetStringChars(env, stringObject);
				eclipseLibrary = _tcsdup(str);
				JNI_ReleaseStringChars(env, stringObject, str);
			}
		}
	}
	if( (*env)->ExceptionOccurred(env) != 0 ){
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}

static void registerNatives(JNIEnv *env) {
	jclass bridge = (*env)->FindClass(env, "org/eclipse/equinox/launcher/JNIBridge");
	if(bridge != NULL) {
		int numNatives = sizeof(natives) / sizeof(natives[0]);
		(*env)->RegisterNatives(env, bridge, natives, numNatives);	
	}
	if( (*env)->ExceptionOccurred(env) != 0 ){
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
}


/* Get a _TCHAR* from a jstring, string should be released later with JNI_ReleaseStringChars */
static const _TCHAR * JNI_GetStringChars(JNIEnv *env, jstring str) {
	const _TCHAR * result = NULL;
#ifdef UNICODE
	/* GetStringChars is not null terminated, make a copy */
	const _TCHAR * stringChars = (*env)->GetStringChars(env, str, 0);
	int length = (*env)->GetStringLength(env, str);
	_TCHAR * copy = malloc( (length + 1) * sizeof(_TCHAR));
	_tcsncpy(copy, stringChars, length);
	copy[length] = _T_ECLIPSE('\0');
	(*env)->ReleaseStringChars(env, str, stringChars);
	result = copy;
#elif MACOSX
	/* Use UTF on the Mac */
	result = (*env)->GetStringUTFChars(env, str, 0);
#else
	/* Other platforms, use java's default encoding */ 
	_TCHAR* buffer = NULL;
	if (string_class == NULL)
		string_class = (*env)->FindClass(env, "java/lang/String");
	if (string_class != NULL) {
		if (string_getBytesMethod == NULL)
			string_getBytesMethod = (*env)->GetMethodID(env, string_class, "getBytes", "()[B");
		if (string_getBytesMethod != NULL) {
			jbyteArray bytes = (*env)->CallObjectMethod(env, str, string_getBytesMethod);
			if (!(*env)->ExceptionOccurred(env)) {
				jsize length = (*env)->GetArrayLength(env, bytes);
				buffer = malloc( (length + 1) * sizeof(_TCHAR*));
				(*env)->GetByteArrayRegion(env, bytes, 0, length, (jbyte*)buffer);
				buffer[length] = 0;
			}
			(*env)->DeleteLocalRef(env, bytes);
		}
	}
	if(buffer == NULL) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	result = buffer;
#endif
	return result;
}

/* Release the string that was obtained using JNI_GetStringChars */
static void JNI_ReleaseStringChars(JNIEnv *env, jstring s, const _TCHAR* data) {
#ifdef UNICODE
	free((_TCHAR*)data);
#elif MACOSX
	(*env)->ReleaseStringUTFChars(env, s, data);
#else
	free((_TCHAR*)data);
#endif
}

static jstring newJavaString(JNIEnv *env, _TCHAR * str)
{
	jstring newString = NULL;
#ifdef UNICODE
	size_t length = _tcslen(str);
	newString = (*env)->NewString(env, str, length);
#elif MACOSX
	newString = (*env)->NewStringUTF(env, str);
#else
	size_t length = _tcslen(str);
	jbyteArray bytes = (*env)->NewByteArray(env, length);
	if(bytes != NULL) {
		(*env)->SetByteArrayRegion(env, bytes, 0, length, (jbyte *)str);
		if (!(*env)->ExceptionOccurred(env)) {
			if (string_class == NULL)
				string_class = (*env)->FindClass(env, "java/lang/String");
			if(string_class != NULL) {
				if (string_ctor == NULL)
					string_ctor = (*env)->GetMethodID(env, string_class, "<init>",  "([B)V");
				if(string_ctor != NULL) {
					newString = (*env)->NewObject(env, string_class, string_ctor, bytes);
				}
			}
		}
		(*env)->DeleteLocalRef(env, bytes);
	}
#endif
	if(newString == NULL) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return newString;
}

static jobjectArray createRunArgs( JNIEnv *env, _TCHAR * args[] ) {
	int index = 0, length = -1;
	jobjectArray stringArray = NULL;
	jstring string;
	
	/*count the number of elements first*/
	while(args[++length] != NULL);
	
	if (string_class == NULL)
		string_class = (*env)->FindClass(env, "java/lang/String");
	if(string_class != NULL) {
		stringArray = (*env)->NewObjectArray(env, length, string_class, 0);
		if(stringArray != NULL) {
			for( index = 0; index < length; index++) {
				string = newJavaString(env, args[index]);
				if(string != NULL) {
					(*env)->SetObjectArrayElement(env, stringArray, index, string); 
					(*env)->DeleteLocalRef(env, string);
				} else {
					(*env)->DeleteLocalRef(env, stringArray);
					(*env)->ExceptionDescribe(env);
					(*env)->ExceptionClear(env);
					return NULL;
				}
			}
		}
	} 
	if(stringArray == NULL) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return stringArray;
}
					 
JavaResults * startJavaJNI( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile )
{
	int i;
	int numVMArgs = -1;
	void * jniLibrary;
	JNI_createJavaVM createJavaVM;
	JavaVMInitArgs init_args;
	JavaVMOption * options;
	char * mainClassName = NULL;
	JavaResults * results = NULL;
	
	/* JNI reflection */
	jclass mainClass = NULL;			/* The Main class to load */
	jmethodID mainConstructor = NULL;	/* Main's default constructor Main() */
	jobject mainObject = NULL;			/* An instantiation of the main class */
	jmethodID runMethod = NULL;			/* Main.run(String[]) */
	jobjectArray methodArgs = NULL;		/* Arguments to pass to run */
	
	results = malloc(sizeof(JavaResults));
	memset(results, 0, sizeof(JavaResults));
	
	jniLibrary = loadLibrary(libPath);
	if(jniLibrary == NULL) {
		results->launchResult = -1;
		results->errorMessage = malloc((_tcslen(failedToLoadLibrary) + _tcslen(libPath) + 1) * sizeof(_TCHAR));
		_stprintf(results->errorMessage, failedToLoadLibrary, libPath);
		return results; /*error*/
	}

	createJavaVM = (JNI_createJavaVM)findSymbol(jniLibrary, _T_ECLIPSE("JNI_CreateJavaVM"));
	if(createJavaVM == NULL) {
		results->launchResult = -2;
		results->errorMessage = malloc((_tcslen(createVMSymbolNotFound) + _tcslen(libPath) + 1) * sizeof(_TCHAR));
		_stprintf(results->errorMessage, createVMSymbolNotFound, libPath);
		return results; /*error*/
	}
	
	/* count the vm args */
	while(vmArgs[++numVMArgs] != NULL) {}
	
	if(numVMArgs <= 0) {
		/*error, we expect at least the required vm arg */
		results->launchResult = -3;
		results->errorMessage = _tcsdup(internalExpectedVMArgs);
		return results;
	}
	
	options = malloc(numVMArgs * sizeof(JavaVMOption));
	for(i = 0; i < numVMArgs; i++){
		options[i].optionString = toNarrow(vmArgs[i]);
		options[i].extraInfo = 0;
	}
		
#ifdef MACOSX
	init_args.version = JNI_VERSION_1_4;
#else		
	init_args.version = JNI_VERSION_1_2;
#endif
	init_args.options = options;
	init_args.nOptions = numVMArgs;
	init_args.ignoreUnrecognized = JNI_TRUE;
	
	if( createJavaVM(&jvm, &env, &init_args) == 0 ) {
		registerNatives(env);
		
		mainClassName = getMainClass(env, jarFile);
		if (mainClassName != NULL) {
			mainClass = (*env)->FindClass(env, mainClassName);
			free(mainClassName);
		}
		
		if (mainClass == NULL) {
			if ((*env)->ExceptionOccurred(env)) {
				(*env)->ExceptionDescribe(env);
				(*env)->ExceptionClear(env);
			}
			mainClass = (*env)->FindClass(env, "org/eclipse/equinox/launcher/Main");
		}	

		if(mainClass != NULL) {
			results->launchResult = -6; /* this will be reset to 0 below on success */
			mainConstructor = (*env)->GetMethodID(env, mainClass, "<init>", "()V");
			if(mainConstructor != NULL) {
				mainObject = (*env)->NewObject(env, mainClass, mainConstructor);
				if(mainObject != NULL) {
					runMethod = (*env)->GetMethodID(env, mainClass, "run", "([Ljava/lang/String;)I");
					if(runMethod != NULL) {
						methodArgs = createRunArgs(env, progArgs);
						if(methodArgs != NULL) {
							results->launchResult = 0;
							results->runResult = (*env)->CallIntMethod(env, mainObject, runMethod, methodArgs);
							(*env)->DeleteLocalRef(env, methodArgs);
						}
					}
					(*env)->DeleteLocalRef(env, mainObject);
				}
			}
		} else {
			results->launchResult = -5;
			results->errorMessage = malloc((_tcslen(mainClassNotFound) + _tcslen(jarFile) + 1) * sizeof(_TCHAR));
			_stprintf(results->errorMessage, mainClassNotFound, jarFile);
		}
		if((*env)->ExceptionOccurred(env)){
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
		
	} else {
		results->launchResult = -4;
		results->errorMessage = _tcsdup(failedCreateVM);
	}

	/* toNarrow allocated new strings, free them */
	for(i = 0; i < numVMArgs; i++){
		free( options[i].optionString );
	}
	free(options);
	return results;
}

static char * getMainClass(JNIEnv *env, _TCHAR * jarFile) {
	jclass jarFileClass = NULL, manifestClass = NULL, attributesClass = NULL;
	jmethodID jarFileConstructor = NULL, getManifestMethod = NULL, getMainAttributesMethod = NULL, closeJarMethod = NULL, getValueMethod = NULL;
	jobject jarFileObject, manifest, attributes;
	jstring mainClassString = NULL;
	jstring jarFileString, headerString;
	const _TCHAR *mainClass;
	
	/* get the classes we need */
	jarFileClass = (*env)->FindClass(env, "java/util/jar/JarFile");
	if (jarFileClass != NULL) {
		manifestClass = (*env)->FindClass(env, "java/util/jar/Manifest");
		if (manifestClass != NULL) {
			attributesClass = (*env)->FindClass(env, "java/util/jar/Attributes");
		}
	}
	if ((*env)->ExceptionOccurred(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (attributesClass == NULL)
		return NULL;
	
	/* find the methods */
	jarFileConstructor = (*env)->GetMethodID(env, jarFileClass, "<init>", "(Ljava/lang/String;Z)V");
	if(jarFileConstructor != NULL) {
		getManifestMethod = (*env)->GetMethodID(env, jarFileClass, "getManifest", "()Ljava/util/jar/Manifest;");
		if(getManifestMethod != NULL) {
			closeJarMethod = (*env)->GetMethodID(env, jarFileClass, "close", "()V");
			if (closeJarMethod != NULL) {
				getMainAttributesMethod = (*env)->GetMethodID(env, manifestClass, "getMainAttributes", "()Ljava/util/jar/Attributes;");
				if (getMainAttributesMethod != NULL) {
					getValueMethod = (*env)->GetMethodID(env, attributesClass, "getValue", "(Ljava/lang/String;)Ljava/lang/String;");
				}
			}
		}
	}
	if ((*env)->ExceptionOccurred(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	if (getValueMethod == NULL)
		return NULL;
	
	/* jarFileString = new String(jarFile); */
	jarFileString = newJavaString(env, jarFile);
	 /* headerString = new String("Main-Class"); */
	 headerString = newJavaString(env, _T_ECLIPSE("Main-Class"));
	if (jarFileString != NULL && headerString != NULL) {
		/* jarfileObject = new JarFile(jarFileString, false); */
		jarFileObject = (*env)->NewObject(env, jarFileClass, jarFileConstructor, jarFileString, JNI_FALSE);
		if (jarFileObject != NULL) { 
			/* manifest = jarFileObject.getManifest(); */
			 manifest = (*env)->CallObjectMethod(env, jarFileObject, getManifestMethod);
			 if (manifest != NULL) {
				 /*jarFileObject.close() */
				 (*env)->CallVoidMethod(env, jarFileObject, closeJarMethod);
				 if (!(*env)->ExceptionOccurred(env)) {
					 /* attributes = manifest.getMainAttributes(); */
					 attributes = (*env)->CallObjectMethod(env, manifest, getMainAttributesMethod);
					 if (attributes != NULL) {
						 /* mainClassString = attributes.getValue(headerString); */
						 mainClassString = (*env)->CallObjectMethod(env, attributes, getValueMethod, headerString);
					 }
				 }
			 }
			 (*env)->DeleteLocalRef(env, jarFileObject);
		}
	}
	
	if (jarFileString != NULL)
		(*env)->DeleteLocalRef(env, jarFileString);
	if (headerString != NULL)
		(*env)->DeleteLocalRef(env, headerString);
	
	if ((*env)->ExceptionOccurred(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	
	if (mainClassString == NULL)
		return NULL;
	
	mainClass = JNI_GetStringChars(env, mainClassString);
	if(mainClass != NULL) {
		int i = -1;
		char *result = toNarrow(mainClass);
		JNI_ReleaseStringChars(env, mainClassString, mainClass);
		
		/* replace all the '.' with '/' */
		while(result[++i] != '\0') {
			if(result[i] == '.')
				result[i] = '/';
		}
		return result;
	}
	return NULL;
}

void cleanupVM(int exitCode) {
	JNIEnv * localEnv = env;
	if (jvm == 0)
		return;
	
	if (secondThread)
		(*jvm)->AttachCurrentThread(jvm, (void**)&localEnv, NULL);
	else
		localEnv = env;
	if (localEnv == 0)
		return;
	
	/* we call System.exit() unless osgi.noShutdown is set */
	if (shouldShutdown(env)) {
		jclass systemClass = NULL;
		jmethodID exitMethod = NULL;
		systemClass = (*env)->FindClass(env, "java/lang/System");
		if (systemClass != NULL) {
			exitMethod = (*env)->GetStaticMethodID(env, systemClass, "exit", "(I)V");
			if (exitMethod != NULL) {
				(*env)->CallStaticVoidMethod(env, systemClass, exitMethod, exitCode);
			}
		}
		if ((*env)->ExceptionOccurred(env)) {
			(*env)->ExceptionDescribe(env);
			(*env)->ExceptionClear(env);
		}
	}
	(*jvm)->DestroyJavaVM(jvm);
}

static int shouldShutdown(JNIEnv * env) {
	jclass booleanClass = NULL;
	jmethodID method = NULL;
	jstring arg = NULL;
	jboolean result = 0;
	
	booleanClass = (*env)->FindClass(env, "java/lang/Boolean");
	if (booleanClass != NULL) {
		method = (*env)->GetStaticMethodID(env, booleanClass, "getBoolean", "(Ljava/lang/String;)Z");
		if (method != NULL) {
			arg = newJavaString(env, _T_ECLIPSE("osgi.noShutdown"));
			result = (*env)->CallStaticBooleanMethod(env, booleanClass, method, arg);
			(*env)->DeleteLocalRef(env, arg);
		}
	}
	if ((*env)->ExceptionOccurred(env)) {
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
	return (result == 0);
}


