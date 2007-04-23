/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/* This file contains code common between GTK & Motif */
#include "eclipseOS.h"
#include "eclipseCommon.h"
#include "eclipseMozilla.h"
#include "eclipseUtil.h"
#include "eclipseJNI.h"

#include <sys/types.h>
#include <sys/stat.h>
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
#ifdef NETSCAPE_FIX
static void   fixEnvForNetscape();
#endif /* NETSCAPE_FIX */

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
	char * buffer;
	char * path;
	char * c;
	char * ldPath;
	char * newPath;
	int i;
	int numPaths = 0;
	int length = 0;
	int needAdjust = 0;
	
	char ** paths = NULL;
#ifdef MOZILLA_FIX
	fixEnvForMozilla();
#endif /* MOZILLA_FIX */
#ifdef NETSCAPE_FIX
	fixEnvForNetscape();
#endif /* NETSCAPE_FIX */
	
	if (eeLibPath != NULL) {
		/*count number of path elements */
		numPaths = 1;
		c = eeLibPath;
		while( (c = strchr(c, pathSeparator)) != NULL) {
			numPaths++;
			c++;
		}
		paths = malloc(numPaths * sizeof(char*));
		path = buffer = _tcsdup(eeLibPath);
		for (i = 0; i < numPaths; i++) {
			c = strchr(path, pathSeparator);
			if(c != NULL) *c++ = 0;
			paths[i] = resolveSymlinks(path);
			length = strlen(paths[i]);
			paths[i] = realloc(paths[i], (length + 2) * sizeof(char));
			paths[i][length] = pathSeparator;
			paths[i][length + 1] = 0;
			path = c;
		}
		free(buffer);
	} else {
		numPaths = 2;
		/* we want the directory containing the library, and the parent directory of that */
		paths = malloc( numPaths * sizeof(char*));
		buffer = strdup(vmLibrary);	
		for (i = 0; i < numPaths; i++) {
			c = strrchr(buffer, dirSeparator);
			*c = 0;
			paths[i] = resolveSymlinks(buffer);
			length = strlen(paths[i]);
			paths[i] = realloc(paths[i], (length + 2) * sizeof(char));
			paths[i][length] = pathSeparator;
			paths[i][length + 1] = 0;
		}
		free(buffer);	
	}
 
	ldPath = (char*)getenv(_T_ECLIPSE("LD_LIBRARY_PATH"));
	if (!ldPath) {
		ldPath = _T_ECLIPSE("");
		needAdjust = 1;
	} else {
		buffer = malloc((strlen(ldPath) + 2) * sizeof(char));
		sprintf(buffer, "%s%c", ldPath, pathSeparator);
		for (i = 0; i < numPaths; i++) {
			c = strstr(buffer, paths[i]);
			if ( c == NULL || !(c == buffer || *(c - 1) == pathSeparator))
			{
				/* entry not found */
				needAdjust = 1;
				break;
			}
		}
		free(buffer);
	}
	if (!needAdjust) {
		for (i = 0; i < numPaths; i++)
			free(paths[i]);
		free(paths);
		return;
	}
	
	/* set the value for LD_LIBRARY_PATH */
	length = strlen(ldPath);
	if (eeLibPath != NULL) {
		newPath = malloc((length + 1 + strlen(eeLibPath) + 1) * sizeof(char));
		sprintf(newPath, "%s%c%s", eeLibPath, pathSeparator, ldPath);
	} else { 
		newPath = malloc((length + 1 + strlen(paths[0]) + 1 + strlen(paths[1]) + 1) * sizeof(char));
		sprintf(newPath, "%s%c%s%c%s", paths[0], pathSeparator, paths[1], pathSeparator, ldPath);
	}
	setenv( "LD_LIBRARY_PATH", newPath, 1);
	
	for (i = 0; i < numPaths; i++)
		free(paths[i]);
	free(paths);
	
	/* now we must restart for this to take affect */
	restartLauncher(initialArgv[0], initialArgv);
}

void restartLauncher( char* program, char* args[] ) 
{
	/* just restart in-place */
	execv( program != NULL ? program : args[0], args);
}

void processVMArgs(_TCHAR **vmargs[] ) {
	/* nothing yet */
}

int startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[] )
{
	return startJavaJNI(libPath, vmArgs, progArgs);
}
