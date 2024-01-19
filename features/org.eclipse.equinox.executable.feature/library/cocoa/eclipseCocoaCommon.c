/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Niefer
 *     Mikael Barbero
 *******************************************************************************/

#include "eclipseCommon.h"
#include "eclipseOS.h"

#include <locale.h>
#include <dlfcn.h>
#include <unistd.h>
#include <CoreServices/CoreServices.h>
#include <Cocoa/Cocoa.h>
#include <mach-o/dyld.h>

char   dirSeparator  = '/';
char   pathSeparator = ':';

static CFBundleRef javaVMBundle = NULL;

/* Initialize Window System
 *
 * Initialize Cocoa.
 */
int initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
	char *homeDir = getProgramDir();
	/*debug("install dir: %s\n", homeDir);*/
	if (homeDir != NULL)
		chdir(homeDir);

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
		inError= CFStringCreateWithCString(kCFAllocatorDefault, title, kCFStringEncodingUTF8);
		inDescription= CFStringCreateWithCString(kCFAllocatorDefault, message, kCFStringEncodingUTF8);
	}

	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	NSAlert *alert = [[NSAlert alloc] init];
    [alert setMessageText:(NSString*)inError];
    [alert setInformativeText:(NSString*)inDescription];
    [alert addButtonWithTitle:@"Ok"];
	[[alert window] setTitle: [NSString stringWithUTF8String: title]];
	[alert setAlertStyle: NSAlertStyleCritical];
	[alert runModal];
	[pool release];
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

static void loadVMBundle( char * bundle ) {
	CFURLRef url = CFURLCreateFromFileSystemRepresentation(kCFAllocatorDefault, (const UInt8 *)bundle, strlen(bundle), true);
	javaVMBundle = CFBundleCreate(kCFAllocatorDefault, url);
	CFRelease(url);
}

/* Load the specified shared library
 */
void * loadLibrary( char * library ){
	if (!isLibrary(library)) {
		loadVMBundle(library);
		return (void*) &javaVMBundle;
	}

	_TCHAR *bundle = strdup(library), *start;

	// check if it's a JVM bundle
	if (strstr(bundle, "libjvm") && (start = strstr(bundle, "/Contents/Home/")) != NULL) {
		start[0] = 0;
		loadVMBundle(bundle);
		free(bundle);
		if (javaVMBundle) {
			return (void*) &javaVMBundle;
		}
	}

	free(bundle);
	void * result= dlopen(library, RTLD_NOW);
	if(result == 0)
		printf("%s\n",dlerror());
	return result;
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

	if(path == NULL)
		return path;

	string = CFStringCreateWithCString(kCFAllocatorDefault, path, kCFStringEncodingUTF8);
	url = CFURLCreateWithFileSystemPath(kCFAllocatorDefault, string, kCFURLPOSIXPathStyle, false);
	CFRelease(string);
	if(url == NULL)
		return path;

	UInt8 fsPath[PATH_MAX];
	if (CFURLGetFileSystemRepresentation(url, true, fsPath, sizeof(fsPath))) {
		NSError *error = nil;
		NSURL *resolvedURL = [NSURL URLByResolvingAliasFileAtURL:(NSURL *)url options:NSURLBookmarkResolutionWithSecurityScope error:&error];
		if (resolvedURL) {
			resolved = CFURLCopyAbsoluteURL(url);
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
