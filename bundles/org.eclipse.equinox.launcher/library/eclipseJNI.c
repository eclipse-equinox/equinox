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
 
#include "eclipseJNI.h"
#include "eclipseCommon.h"
#include "eclipseOS.h"
#include <stdlib.h>
  
  
_TCHAR * exitData = NULL;

static JNINativeMethod natives[] = {{"_update_splash", "()V", &update_splash},
									{"_get_splash_handle", "()I", &get_splash_handle},
									{"_set_exit_data", "(Ljava/lang/String;)V", &set_exit_data}};
  
/* local methods */
static jstring newJavaString(JNIEnv *env, _TCHAR * str);

#ifndef UNICODE /* we only want one version of these functions */
/* JNI Callback methods */
JNIEXPORT void JNICALL set_exit_data(JNIEnv * env, jobject obj, jstring s){
	setExitData(env, s);
}

JNIEXPORT void JNICALL update_splash(JNIEnv * env, jobject obj){
	
}

JNIEXPORT jint JNICALL get_splash_handle(JNIEnv * env, jobject obj){
	return 0;
}
#endif

void setExitData(JNIEnv *env, jstring s){ 
	int length = (*env)->GetStringLength(env, s);
	_TCHAR * copy = malloc(length * sizeof(_TCHAR*));
	const _TCHAR * data;
	
#ifdef UNICODE
	/* TODO need to figure out how to get a unicode version called */
	data = (*env)->GetStringChars(env, s, 0);
	_tcsncpy( copy, data, length);
	(*env)->ReleaseStringChars(env, s, data);
#else 
	data = (*env)->GetStringUTFChars(env, s, 0);
	_tcscpy( copy, data );
	(*env)->ReleaseStringUTFChars(env, s, data);
#endif	
	exitData = copy;
}

static jstring newJavaString(JNIEnv *env, _TCHAR * str)
{
	jstring newString = 0;
	int length = _tcslen(str);
	
#ifdef UNICODE
	newString = (*env)->NewString(env, str, length);
#else
	jbyteArray bytes = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, bytes, 0, length, str);
	if (!(*env)->ExceptionOccurred(env)) {
		jclass stringClass = (*env)->FindClass(env, "java/lang/String");
		jmethodID ctor = (*env)->GetMethodID(env, stringClass, "<init>",  "([B)V");
	    newString = (*env)->NewObject(env, stringClass, ctor, bytes);
	}
	(*env)->DeleteLocalRef(env, bytes);
#endif
	
	return newString;
}

static jobjectArray createRunArgs( JNIEnv *env, _TCHAR * args[] ) {
	int index = 0, length = -1;
	
	/*count the number of elements first*/
	while(args[++length] != NULL);
	
	jclass stringClass = (*env)->FindClass(env, "java/lang/String");
	jobjectArray stringArray = (*env)->NewObjectArray(env, length, stringClass, 0);
	for( index = 0; index < length; index++) {
		jstring string = newJavaString(env, args[index]);
		(*env)->SetObjectArrayElement(env, stringArray, index, string); 
		(*env)->DeleteLocalRef(env, string);
	}
	return stringArray;
}

/**
 * Convert a wide string to a narrow one suitable for use in JNI.
 * Caller must free the null terminated string returned.
 */
static char *toNarrow(_TCHAR* src)
{
#ifdef UNICODE
	int byteCount = WideCharToMultiByte (CP_ACP, 0, (wchar_t *)src, -1, NULL, 0, NULL, NULL);
	char *dest = malloc(byteCount+1);
	dest[byteCount] = 0;
	WideCharToMultiByte (CP_ACP, 0, (wchar_t *)src, -1, dest, byteCount, NULL, NULL);
	return dest;
#else
	return _tcsdup(src);
#endif
}
 							 
int startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[] )
{
	int i;
	int numVMArgs = -1;
	int jvmExitCode = 0;
	void * jniLibrary;
	JNI_createJavaVM createJavaVM;
	JavaVMInitArgs init_args;
	JavaVMOption * options;
	JavaVM * jvm;
	JNIEnv *env;
	
	jniLibrary = loadLibrary(libPath);
	if(jniLibrary == NULL) {
		return -1; /*error*/
	}
	
	createJavaVM = findSymbol(jniLibrary, "JNI_CreateJavaVM");
	if(createJavaVM == NULL) {
		return -1; /*error*/
	}
	
	/* count the vm args */
	while(vmArgs[++numVMArgs] != NULL) {}
	
	options = malloc(numVMArgs * sizeof(JavaVMOption));
	for(i = 0; i < numVMArgs; i++){
		options[i].optionString = toNarrow(vmArgs[i]);
		options[i].extraInfo = 0;
	}
		
	init_args.version = JNI_VERSION_1_2;
	init_args.options = options;
	init_args.nOptions = numVMArgs;
	init_args.ignoreUnrecognized = JNI_TRUE;
	
	if( createJavaVM(&jvm, &env, &init_args) == 0 ) {
		jclass bridge = (*env)->FindClass(env, "org/eclipse/core/launcher/JNIBridge");
		int numNatives = sizeof(natives) / sizeof(natives[0]);
		(*env)->RegisterNatives(env, bridge, natives, numNatives);
		
		jclass mainClass = (*env)->FindClass(env, "org/eclipse/core/launcher/Main");
		jmethodID mainConstructor = (*env)->GetMethodID(env, mainClass, "<init>", "()V");
		jobject mainObject = (*env)->NewObject(env, mainClass, mainConstructor);
		jmethodID runMethod = (*env)->GetMethodID(env, mainClass, "run", "([Ljava/lang/String;)I");
		if(runMethod != NULL) {
			jobjectArray methodArgs = createRunArgs(env, progArgs);
			jvmExitCode = (*env)->CallIntMethod(env, mainObject, runMethod, methodArgs);
		}
		/*(*jvm)->DestroyJavaVM(jvm);*/ 
	}
	unloadLibrary(jniLibrary);
	free(progArgs);

	/* toNarrow allocated new strings, free them */
	for(i = 0; i < numVMArgs; i++){
		free( options[i].optionString );
	}
	
	return jvmExitCode;
}



