/*
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *	  Andrew Niefer
 */

#include "eclipseCommon.h"

#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <ctype.h>
#include <pwd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <CoreServices/CoreServices.h>
#include <Carbon/Carbon.h>
#include <mach-o/dyld.h>


#define APP_PACKAGE_PATTERN ".app/Contents/MacOS/"
#define APP_PACKAGE "APP_PACKAGE"
#define JAVAROOT "JAVAROOT"

static void debug(const char *fmt, ...);
static void dumpArgs(char *tag, int argc, char* argv[]);
static char *append(char *buffer, const char *s);
static char *appendc(char *buffer, char c);
static char *expandShell(char *arg, const char *appPackage, const char *javaRoot);
static char *my_strcasestr(const char *big, const char *little);

static FILE *fgConsoleLog;
static char *fgAppPackagePath;
static int fgPid;

extern int original_main(int argc, char* argv[]);
int main( int argc, char* argv[] ) {

	SInt32 systemVersion= 0;
	if (Gestalt(gestaltSystemVersion, &systemVersion) == noErr) {
		systemVersion &= 0xffff;
#ifdef COCOA
		if (systemVersion < 0x1050) {
			displayMessage("Error", "This application requires Mac OS X 10.5 (Leopard) or greater.");
			return 0;
		}
#else
		if (systemVersion < 0x1020) {
			displayMessage("Error", "This application requires Jaguar (Mac OS X >= 10.2)");
			return 0;
		}
#endif
	}

	fgConsoleLog= fopen("/dev/console", "w");
	fgPid= getpid();

	dumpArgs("start", argc, argv);
	if ( (argc > 1 && strncmp(argv[1], "-psn_", 5) == 0) || argc == 1) {
		/* find path to application bundle (ignoring case) */
		char *pos= my_strcasestr(argv[0], APP_PACKAGE_PATTERN);
		if (pos != NULL) {
			int l= pos-argv[0] + 4;	// reserve space for ".app"
			fgAppPackagePath= malloc(l+1);
			strncpy(fgAppPackagePath, argv[0], l);
			fgAppPackagePath[l]= '\0';	// terminate result
		}
		
		/* Get the main bundle for the app */
		CFBundleRef mainBundle= CFBundleGetMainBundle();
		if (mainBundle != NULL) {
		
			/* Get an instance of the info plist.*/
			CFDictionaryRef bundleInfoDict= CFBundleGetInfoDictionary(mainBundle);
						
			/* If we succeeded, look for our property. */
			if (bundleInfoDict != NULL) {
				CFArrayRef ar= CFDictionaryGetValue(bundleInfoDict, CFSTR("Eclipse"));
				if (ar) {
					CFIndex size= CFArrayGetCount(ar);
					if (size > 0) {
						int i;
						char **old_argv= argv;
						argv= (char**) calloc(size+2, sizeof(char*));
						argc= 0;
						argv[argc++]= old_argv[0];
						for (i= 0; i < size; i++) {
							CFStringRef sr= (CFStringRef) CFArrayGetValueAtIndex (ar, i);
							CFIndex argStringSize= CFStringGetMaximumSizeForEncoding(CFStringGetLength(sr), kCFStringEncodingUTF8);
							char *s= malloc(argStringSize);
							if (CFStringGetCString(sr, s, argStringSize, kCFStringEncodingUTF8)) {
								argv[argc++]= expandShell(s, fgAppPackagePath, NULL);
							} else {
								fprintf(fgConsoleLog, "can't extract bytes\n");
							}
							//free(s);
						}
					}
				} else {
					fprintf(fgConsoleLog, "no Eclipse dict found\n");
				}
			} else {
				fprintf(fgConsoleLog, "no bundle dict found\n");
			}
		} else {
			fprintf(fgConsoleLog, "no bundle found\n");
		}
	}
	int exitcode= original_main(argc, argv);
	debug("<<<< exit(%d)\n", exitcode);
	fclose(fgConsoleLog);
	return exitcode;
}

static void debug(const char *fmt, ...) {
#if DEBUG
	va_list ap;
	va_start(ap, fmt);
	fprintf(fgConsoleLog, "%05d: ", fgPid);
	vfprintf(fgConsoleLog, fmt, ap);
	va_end(ap);
#endif
}

static void dumpArgs(char *tag, int argc, char* argv[]) {
#if DEBUG
	int i;
	if (argc < 0) {
		argc= 0;
		for (i= 0; argv[i] != NULL; i++)
			 argc++;
	}
	debug(">>>> %s:", tag);
	for (i= 0; i < argc && argv[i] != NULL; i++)
		fprintf(fgConsoleLog, " <%s>", argv[i]);
	fprintf(fgConsoleLog, "\n");
#endif
}

/*
 * Expand $APP_PACKAGE, $JAVA_HOME, and does tilde expansion.
 
	A word beginning with an unquoted tilde character (~) is
	subject to tilde expansion. All the characters up to a
	slash (/) or the end of the word are treated as a username
	and are replaced with the user's home directory. If the
	username is missing (as in ~/foobar), the tilde is
	replaced with the value of the HOME variable (the current
	user's home directory).
 */
static char *expandShell(char *arg, const char *appPackage, const char *javaRoot) {
	
	if (index(arg, '~') == NULL && index(arg, '$') == NULL)
		return arg;
	
	char *buffer= strdup("");
	char c, lastChar= ' ';
	const char *cp= arg;
	while ((c = *cp++) != 0) {
		if (isspace(lastChar) && c == '~') {
			char name[100], *dir= NULL;
			int j= 0;
			for (; (c = *cp) != 0; cp++) {
				if (! isalnum(c))
					break;
				name[j++]= c;
				lastChar= c;
			}
			name[j]= '\0';
			if (j > 0) {
				struct passwd *pw= getpwnam(name);
				if (pw != NULL)
					dir= pw->pw_dir;
			} else {
				dir= getenv("HOME");
			}
			if (dir != NULL)
				buffer= append(buffer, dir);
				
		} else if (c == '$') {
			int l= strlen(APP_PACKAGE);
			if (appPackage != NULL && strncmp(cp, APP_PACKAGE, l) == 0) {
				cp+= l;
				buffer= append(buffer, appPackage);
			} else {
				int l= strlen(JAVAROOT);
				if (javaRoot != NULL && strncmp(cp, JAVAROOT, l) == 0) {
					cp+= l;
					buffer= append(buffer, javaRoot);
				} else {
					buffer= appendc(buffer, c);
				}
			}
		} else
			buffer= appendc(buffer, c);
		lastChar= c;
	}
	return buffer;
}

static char *my_strcasestr(const char *big, const char *little) {
    char *cp, *s, *t;
    for (cp= (char*) big; *cp; cp++) {
        for (s= cp, t= (char*) little; *s && *t; s++, t++)
            if (toupper(*s) != toupper(*t))
                break;
        if (*t == '\0')
            return cp;
    }
    return NULL;
}

static char *append(char *buffer, const char *s) {
	int bl= strlen(buffer);
	int sl= strlen(s);
	buffer= realloc(buffer, bl+sl+1);
	strcpy(&buffer[bl], s);
	return buffer;
}

static char *appendc(char *buffer, char c) {
	int bl= strlen(buffer);
	buffer= realloc(buffer, bl+2);
	buffer[bl++]= c;
	buffer[bl]= '\0';
	return buffer;
}