/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
#ifdef COCOA
#include <Cocoa/Cocoa.h>
#else
#include <Carbon/Carbon.h>
#endif
#include <mach-o/dyld.h>

char   dirSeparator  = '/';
char   pathSeparator = ':';

static CFBundleRef javaVMBundle = NULL;

int initialized = 0;

static void init() {
	if (!initialized) {
		ProcessSerialNumber psn;
		if (GetCurrentProcess(&psn) == noErr) {
			TransformProcessType(&psn, kProcessTransformToForegroundApplication);
			SetFrontProcess(&psn);
		}
#ifdef COCOA
		[NSApplication sharedApplication];
#else
		ClearMenuBar();
#endif
		initialized= true;
	}
}


/* Initialize Window System
 *
 * Initialize Carbon.
 */
int initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
	char *homeDir = getProgramDir();
	/*debug("install dir: %s\n", homeDir);*/
	if (homeDir != NULL)
		chdir(homeDir);
    
	if (showSplash)
		init();
	
	return 0;
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
		inError= CFStringCreateWithCString(kCFAllocatorDefault, buffer, kCFStringEncodingUTF8);
		free(buffer);
		inDescription= CFStringCreateWithCString(kCFAllocatorDefault, pos+2, kCFStringEncodingUTF8);
	} else {
		inError= CFStringCreateWithCString(kCFAllocatorDefault, message, kCFStringEncodingUTF8);
	}
	
	init();
	
#ifdef COCOA
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	NSAlert* alert = [NSAlert alertWithMessageText: (NSString*)(inDescription != nil ? inError : nil) defaultButton: nil alternateButton: nil otherButton: nil informativeTextWithFormat: (NSString*)(inDescription != nil ? inDescription : inError)];
	[[alert window] setTitle: [NSString stringWithUTF8String: title]];
	[alert setAlertStyle: NSCriticalAlertStyle];
	[alert runModal];
	[pool release];
#else
	DialogRef outAlert;
	OSStatus status= CreateStandardAlert(kAlertStopAlert, inError, inDescription, NULL, &outAlert);
	if (status == noErr) {
		DialogItemIndex outItemHit;
		RunStandardAlert(outAlert, NULL, &outItemHit);
	} else {
		/*debug("%s: displayMessage: %s\n", title, message);*/
	}
#endif
	CFRelease(inError);
	if (inDescription != NULL)
		CFRelease(inDescription);
}

static int isLibrary( _TCHAR* vm ){
	_TCHAR *ch = NULL;
	if (vm == NULL) return 0;
	ch = _tcsrchr( vm, '.' );
	if(ch == NULL)
		return 0;
	return (_tcsicmp(ch, _T_ECLIPSE(".so")) == 0) || (_tcsicmp(ch, _T_ECLIPSE(".jnilib")) == 0) || (_tcsicmp(ch, _T_ECLIPSE(".dylib")) == 0);
}

/* Load the specified shared library
 */
void * loadLibrary( char * library ){
	if (!isLibrary(library)) {
		CFURLRef url = CFURLCreateFromFileSystemRepresentation(kCFAllocatorDefault, (const UInt8 *)library, strlen(library), true);
		javaVMBundle = CFBundleCreate(kCFAllocatorDefault, url);
		CFRelease(url);
		return (void*) &javaVMBundle;
	} else {
		void * result= dlopen(library, RTLD_NOW);
		if(result == 0) 
			printf("%s\n",dlerror());
		return result;
	}
}

/* Unload the shared library
 */
void unloadLibrary( void * handle ){
	if (handle == &javaVMBundle)
		CFRelease(javaVMBundle);
	else
		dlclose(handle);
}
 
/* Find the given symbol in the shared library
 */
void * findSymbol( void * handle, char * symbol ){
	if(handle == &javaVMBundle) {
		CFStringRef string = CFStringCreateWithCString(kCFAllocatorDefault, symbol, kCFStringEncodingASCII);
		void * ptr = CFBundleGetFunctionPointerForName(javaVMBundle, string);
		CFRelease(string);
		return ptr;
	} else
		return dlsym(handle, symbol);
}

char * resolveSymlinks( char * path ) {
	char * result = 0;
	CFURLRef url, resolved;
	CFStringRef string;
	FSRef fsRef;
	Boolean isFolder, wasAliased;
	
	if(path == NULL)
		return path;
		
	string = CFStringCreateWithCString(kCFAllocatorDefault, path, kCFStringEncodingUTF8);
	url = CFURLCreateWithFileSystemPath(kCFAllocatorDefault, string, kCFURLPOSIXPathStyle, false);
	CFRelease(string);
	if(url == NULL)
		return path;
	
	if(CFURLGetFSRef(url, &fsRef)) {
		if( FSResolveAliasFile(&fsRef, true, &isFolder, &wasAliased) == noErr) {
			resolved = CFURLCreateFromFSRef(kCFAllocatorDefault, &fsRef);
			if(resolved != NULL) {
				string = CFURLCopyFileSystemPath(resolved, kCFURLPOSIXPathStyle);
				CFIndex length = CFStringGetMaximumSizeForEncoding(CFStringGetLength(string), kCFStringEncodingUTF8);
				char *s = malloc(length);
				if (CFStringGetCString(string, s, length, kCFStringEncodingUTF8)) {
					result = s;
				} else {
					free(s);
				}
				CFRelease(string);
				CFRelease(resolved);
			}
		}
	}	
	CFRelease(url); 
	return result;
}