/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Niefer
 *******************************************************************************/
 
#include "eclipseCommon.h"
#include "eclipseOS.h"

#include <locale.h>
#include <dlfcn.h>
#include <unistd.h>
#include <CoreServices/CoreServices.h>
#include <Carbon/Carbon.h>
#include <mach-o/dyld.h>

char   dirSeparator  = '/';
char   pathSeparator = ':';

void initWindowSystem( int* pArgc, _TCHAR* argv[], int showSplash );

int initialized = 0;

static void init() {
	if (!initialized) {
		ProcessSerialNumber psn;
		if (GetCurrentProcess(&psn) == noErr) {
			TransformProcessType(&psn, kProcessTransformToForegroundApplication);
			SetFrontProcess(&psn);
		}
		ClearMenuBar();
		initialized= true;
	}
}


/* Initialize Window System
 *
 * Initialize Carbon.
 */
void initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
	char *homeDir = getProgramDir();
	/*debug("install dir: %s\n", homeDir);*/
	if (homeDir != NULL)
		chdir(homeDir);
    
	if (showSplash)
		init();
}

/* Display a Message */
void displayMessage(char *title, char *message)
{
	CFStringRef inError, inDescription= NULL;

	/* try to break the message into a first sentence and the rest */
	char *pos= strstr(message, ". ");
	if (pos != NULL) {	
		char *to, *from, *buffer= calloc(pos-message+2, sizeof(char));
		/* copy and replace line separators with blanks */
		for (to= buffer, from= message; from <= pos; from++, to++) {
			char c= *from;
			if (c == '\n') c= ' ';
			*to= c;
		}
		inError= CFStringCreateWithCString(kCFAllocatorDefault, buffer, kCFStringEncodingASCII);
		free(buffer);
		inDescription= CFStringCreateWithCString(kCFAllocatorDefault, pos+2, kCFStringEncodingASCII);
	} else {
		inError= CFStringCreateWithCString(kCFAllocatorDefault, message, kCFStringEncodingASCII);
	}
	
	init();
	
	DialogRef outAlert;
	OSStatus status= CreateStandardAlert(kAlertStopAlert, inError, inDescription, NULL, &outAlert);
	if (status == noErr) {
		DialogItemIndex outItemHit;
		RunStandardAlert(outAlert, NULL, &outItemHit);
	} else {
		/*debug("%s: displayMessage: %s\n", title, message);*/
	}
	CFRelease(inError);
	if (inDescription != NULL)
		CFRelease(inDescription);
}

/* Load the specified shared library
 */
void * loadLibrary( char * library ){
	void * result= dlopen(library, RTLD_NOW);
	if(result == 0) 
		printf("%s\n",dlerror());
	return result;
}

/* Unload the shared library
 */
void unloadLibrary( void * handle ){
	dlclose(handle);
}
 
/* Find the given symbol in the shared library
 */
void * findSymbol( void * handle, char * symbol ){
	return dlsym(handle, symbol);
}
