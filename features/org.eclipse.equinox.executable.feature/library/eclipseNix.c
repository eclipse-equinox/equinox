/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
 *******************************************************************************/

/* This file contains code common between GTK & Motif */
#include "eclipseOS.h"
#include "eclipseCommon.h"
#include "eclipseUtil.h"
#include "eclipseJNI.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#ifdef i386
#define JAVA_ARCH "i386"
#elif defined(__ppc__) || defined(__powerpc64__)
#define JAVA_ARCH "ppc" 
#elif defined(SOLARIS)
#define JAVA_ARCH "sparc"
#elif defined(__amd64__) || defined(__x86_64__) 
#define JAVA_ARCH "amd64"
#else
#define JAVA_ARCH DEFAULT_OS_ARCH
#endif

#ifdef AIX
#define LIB_PATH_VAR _T_ECLIPSE("LIBPATH")
#else
#define LIB_PATH_VAR _T_ECLIPSE("LD_LIBRARY_PATH")
#endif

#define MAX_LOCATION_LENGTH 40 /* none of the jvmLocations strings should be longer than this */ 
static const char* jvmLocations [] = { "j9vm", "../jre/bin/j9vm",
									   "classic", "../jre/bin/classic",
									   "../lib/" JAVA_ARCH "/client",  
									   "../lib/" JAVA_ARCH "/server",
									   "../lib/" JAVA_ARCH "/jrockit",
									   "../jre/lib/" JAVA_ARCH "/client",
									   "../jre/lib/" JAVA_ARCH "/server",
									   "../jre/lib/" JAVA_ARCH "/jrockit",
									   "../lib/jvm/jre/lib/" JAVA_ARCH "/client",
								 	   NULL };

static void adjustLibraryPath( char * vmLibrary );
static char * findLib(char * command);

char * findVMLibrary( char* command ) {
	char * lib = findLib(command);
	if( lib != NULL ) {
		adjustLibraryPath(lib);
	}
	return lib;
}

static char * findLib(char * command) {
	int i;
	int pathLength;	
	struct stat stats;
	char * path;				/* path to resulting jvm shared library */
	char * location;			/* points to begining of jvmLocations section of path */
	
	if (command != NULL) {
		/*check first to see if command already points to the library */
		if (isVMLibrary(command)) {
			if (stat( command, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return strdup(command);
			}
			return NULL;
		}
		
		location = strrchr( command, dirSeparator ) + 1;
		pathLength = location - command;
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + strlen(vmLibrary) + 1) * sizeof(char));
		strncpy(path, command, pathLength);
		location = &path[pathLength];
		 
		/* 
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		i = -1;
		while(jvmLocations[++i] != NULL) {
			sprintf(location, "%s%c%s", jvmLocations[i], dirSeparator, vmLibrary);
			if (stat( path, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return path;
			}
		}
	}
	return NULL;
}

/* adjust the LD_LIBRARY_PATH for the vmLibrary */
static void adjustLibraryPath( char * vmLibrary ) {
	char * c;
	char * ldPath;
	char * newPath;
	int i;
	int numPaths = 0;
	int length = 0;
	int needAdjust = 0;
	
	char ** paths = NULL;
	
	paths = getVMLibrarySearchPath(vmLibrary);
 
	ldPath = (char*)getenv(LIB_PATH_VAR);
	if (!ldPath) {
		ldPath = _T_ECLIPSE("");
		needAdjust = 1;
	} else {
		needAdjust = !containsPaths(ldPath, paths);
	}
	if (!needAdjust) {
		for (i = 0; paths[i] != NULL; i++)
			free(paths[i]);
		free(paths);
		return;
	}
	
	/* set the value for LD_LIBRARY_PATH */
	length = strlen(ldPath);
	c = concatStrings(paths);
	newPath = malloc((_tcslen(c) + length + 1) * sizeof(_TCHAR));
	_stprintf(newPath, _T_ECLIPSE("%s%s"), c, ldPath);
	
	setenv( LIB_PATH_VAR, newPath, 1);
	free(newPath);
	free(c);
	
	for (i = 0; i < numPaths; i++)
		free(paths[i]);
	free(paths);

	/* now we must restart for this to take affect */
	restartLauncher(initialArgv[0], initialArgv);
}

void restartLauncher( char* program, char* args[] ) 
{
	/* just restart in-place */
	execvp( program != NULL ? program : args[0], args);
}

void processVMArgs(_TCHAR **vmargs[] ) {
	/* nothing yet */
}

JavaResults* startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile )
{
	return startJavaJNI(libPath, vmArgs, progArgs, jarFile);
}

/* returns 1 if the JVM version is >= 9, 0 otherwise */
int isModularVM( _TCHAR * javaVM, _TCHAR * jniLib ) {
	if (javaVM == NULL) {
		return 0;
	}
	FILE *fp = NULL;
	_TCHAR buffer[4096];
	_TCHAR *version = NULL, *firstChar;
	int numChars = 0, result = 0;
	_stprintf(buffer,"%s -version 2>&1", javaVM);
	fp = popen(buffer, "r");
	if (fp == NULL) {
		return 0;
	}
	while (fgets(buffer, sizeof(buffer)-1, fp) != NULL) {
		if (!version) {
			firstChar = (_TCHAR *) (_tcschr(buffer, '"') + 1);
			if (firstChar != NULL)
				numChars = (int)  (_tcsrchr(buffer, '"') - firstChar);

			/* Allocate a buffer and copy the version string into it. */
			if (numChars > 0) {
				version = malloc( numChars + 1 );
				_tcsncpy(version, firstChar, numChars);
				version[numChars] = '\0';
			}
		}
		if (version != NULL) {
			_TCHAR *str = version;
			/* According to the new Java version-string scheme, the first element is
			 * the major version number, details at http://openjdk.java.net/jeps/223 */
			_TCHAR *majorVersion = _tcstok(str, ".-");
			if (majorVersion != NULL && (_tcstol(majorVersion, NULL, 10) >= 9)) {
				result = 1;
			}
			free(version);
		}
		break;
	}
	pclose(fp);
	return result;
}
