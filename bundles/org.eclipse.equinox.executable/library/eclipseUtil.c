/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *	   Martin Oberhuber (Wind River) - [149994] Add --launcher.appendVmargs
 *******************************************************************************/

/* Eclipse Launcher Utility Methods */

#include "eclipseOS.h"
#include "eclipseCommon.h"
#include "eclipseUtil.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/stat.h>
#ifdef _WIN32
#include <direct.h>
#else
#include <unistd.h>
#include <strings.h>
#endif

#define MAX_LINE_LENGTH 256

/* Is the given VM J9 */
int isJ9VM( _TCHAR* vm )
{
	_TCHAR * ch = NULL, *ch2 = NULL;
	int res = 0;
	
	if (vm == NULL)
		return 0;
	
	ch = lastDirSeparator( vm );
	if (isVMLibrary(vm)) {
		/* a library, call it j9 if the parent dir is j9vm */
		if(ch == NULL)
			return 0;
		ch[0] = 0;
		ch2 = lastDirSeparator(vm);
		if(ch2 != NULL) {
			res = (_tcsicmp(ch2 + 1, _T_ECLIPSE("j9vm")) == 0);
		}
		ch[0] = dirSeparator;
		return res;
	} else {
		if (ch == NULL)
		    ch = vm;
		else
		    ch++;
		return (_tcsicmp( ch, _T_ECLIPSE("j9") ) == 0);
	}
}

int checkProvidedVMType( _TCHAR* vm ) 
{
	_TCHAR* ch = NULL;
	struct _stat stats;
	
	if (vm == NULL) return VM_NOTHING;
	
	if (_tstat(vm, &stats) == 0 && (stats.st_mode & S_IFDIR) != 0) {
		/* directory */
		return VM_DIRECTORY;
	}

	ch = _tcsrchr( vm, _T_ECLIPSE('.') );
	if(ch == NULL)
		return VM_OTHER;
	
#ifdef _WIN32
	if (_tcsicmp(ch, _T_ECLIPSE(".dll")) == 0)
#else
	if ((_tcsicmp(ch, _T_ECLIPSE(".so")) == 0) || (_tcsicmp(ch, _T_ECLIPSE(".jnilib")) == 0) || (_tcsicmp(ch, _T_ECLIPSE(".dylib")) == 0))
#endif
	{
		return VM_LIBRARY;
	}
	
	if (_tcsicmp(ch, _T_ECLIPSE(".ee")) == 0)
		return VM_EE_PROPS;
	
	return VM_OTHER;
}

/*
 * pathList is a pathSeparator separated list of paths, run each through
 * checkPath and recombine the results.
 * New memory is always allocated for the result
 */
_TCHAR * checkPathList( _TCHAR* pathList, _TCHAR* programDir, int reverseOrder) {
	_TCHAR * c1, *c2;
	_TCHAR * checked, *result;
	size_t checkedLength = 0, resultLength = 0;
	size_t bufferLength = _tcslen(pathList);
	
	result = malloc(bufferLength * sizeof(_TCHAR));
	c1 = pathList;
    while (c1 != NULL && *c1 != _T_ECLIPSE('\0'))
    {
    	c2 = _tcschr(c1, pathSeparator);
		if (c2 != NULL)
			*c2 = 0;
		
		checked = checkPath(c1, programDir, reverseOrder);
		checkedLength = _tcslen(checked);
		if (resultLength + checkedLength + 1> bufferLength) {
			bufferLength += checkedLength + 1;
			result = realloc(result, bufferLength * sizeof(_TCHAR));
		}
		
		if(resultLength > 0) {
			result[resultLength++] = pathSeparator;
			result[resultLength] = _T_ECLIPSE('\0');
		}
		_tcscpy(result + resultLength, checked);
		resultLength += checkedLength;
		
		if(checked != c1)
			free(checked);
		if(c2 != NULL)
			*(c2++) = pathSeparator;
		c1 = c2;
	}
    
    return result;
}

_TCHAR * concatStrings(_TCHAR**strs) {
	return concatPaths(strs, 0);
}

_TCHAR * concatPaths(_TCHAR** strs, _TCHAR separator) {
	_TCHAR separatorString[] = { separator, 0 };
	_TCHAR * result;
	int i = -1;
	size_t length = 0;
	/* first count how large a buffer we need */
	while (strs[++i] != NULL) {
		length += _tcslen(strs[i]) + (separator != 0 ? 1 : 0);
	}

	result = malloc((length + 1) * sizeof(_TCHAR));
	result[0] = 0;
	i = -1;
	while (strs[++i] != NULL) {
		result = _tcscat(result, strs[i]);
		if (separator != 0)
			result = _tcscat(result, separatorString);
	}
	return result;
}

/*
 * Concatenates two NULL-terminated arrays of Strings,
 * returning a new NULL-terminated array.
 * The returned array must be freed with the regular free().
 */
_TCHAR** concatArgs(_TCHAR** l1, _TCHAR** l2) {
	_TCHAR** newArray = NULL;
	int size1 = 0;
	int size2 = 0;

	if (l1 != NULL)
		while (l1[size1] != NULL) size1++;
	if (l2 != NULL)
		while (l2[size2] != NULL) size2++;

	newArray = (_TCHAR **) malloc((size1 + size2 + 1) * sizeof(_TCHAR *));
	if (size1 > 0) {
		memcpy(newArray, l1, size1 * sizeof(_TCHAR *));
	}
	if (size2 > 0) {
		memcpy(newArray + size1, l2, size2 * sizeof(_TCHAR *));
	}
	newArray[size1 + size2] = NULL;
	return newArray;
}

/*
 * returns the relative position of arg in the NULL-terminated list of args,
 * or -1 if args does not contain arg.
 */
int indexOf(_TCHAR *arg, _TCHAR **args) {
	int i = -1;
	if (arg != NULL && args != NULL) {
		while (args[++i] != NULL) {
			if (_tcsicmp(arg, args[i]) == 0) {
				return i;
			}
		}
	}
	return -1;
}

/*
 * buffer contains a pathSeparator separated list of paths, check 
 * that it contains all the paths given.  Each path is expected to be
 * terminated with a pathSeparator character.
 */
int containsPaths(_TCHAR * str, _TCHAR** paths) {
	_TCHAR * buffer;
	_TCHAR * c;
	int i;
	
	/* terminate the string with a pathSeparator */
	buffer = malloc((_tcslen(str) + 2) * sizeof(_TCHAR));
	_stprintf(buffer, _T_ECLIPSE("%s%c"), str, pathSeparator);
	
	for (i = 0; paths[i] != NULL; i++) {
		c = _tcsstr(buffer, paths[i]);
		if ( c == NULL || !(c == buffer || *(c - 1) == pathSeparator))
		{
			/* entry not found */
			free(buffer);
			return 0;
		}
	}
	free(buffer);
	return 1;
}

int isVMLibrary( _TCHAR* vm )
{
	_TCHAR *ch = NULL;
	if (vm == NULL) return 0;
	ch = _tcsrchr( vm, '.' );
	if(ch == NULL)
		return 0;
#ifdef _WIN32
	return (_tcsicmp(ch, _T_ECLIPSE(".dll")) == 0);
#else
	return (_tcsicmp(ch, _T_ECLIPSE(".so")) == 0) || (_tcsicmp(ch, _T_ECLIPSE(".jnilib")) == 0) || (_tcsicmp(ch, _T_ECLIPSE(".dylib")) == 0);
#endif
}

#ifdef AIX

#include <sys/types.h>
#include <time.h>

/* Return the JVM version in the format x.x.x 
 */
char* getVMVersion( char *vmPath )
{
    char   cmd[MAX_LINE_LENGTH];
    char   lineString[MAX_LINE_LENGTH];
    char*  firstChar;
    char   fileName[MAX_LINE_LENGTH];
    time_t curTime;
    FILE*  fp;
    int    numChars = 0;
    char*  version  = NULL;

	/* Define a unique filename for the java output. */
    (void) time(&curTime);
    (void) sprintf(fileName, "/tmp/tmp%ld.txt", curTime);

    /* Write java -version output to a temp file */
    (void) sprintf(cmd,"%s -version 2> %s", vmPath, fileName);
    (void) system(cmd); 

    fp = fopen(fileName, "r");
    if (fp != NULL)
    {
    	/* Read java -version output from a temp file */
    	if (fgets(lineString, MAX_LINE_LENGTH, fp) == NULL)
    		lineString[0] = '\0';
    	fclose(fp);
    	unlink(fileName);

    	/* Extract version number */
    	firstChar = (char *) (strchr(lineString, '"') + 1);
    	if (firstChar != NULL)
    		numChars = (int)  (strrchr(lineString, '"') - firstChar);
    	
    	/* Allocate a buffer and copy the version string into it. */
    	if (numChars > 0)
    	{
    		version = malloc( numChars + 1 );
    		strncpy(version, firstChar, numChars);
			version[numChars] = '\0';
		}
	}  

    return version;
}
#endif /* AIX */

/* Compare JVM Versions of the form "x.x.x..."
 *     
 *    Returns -1 if ver1 < ver2
 *    Returns  0 if ver1 = ver2 
 *    Returns  1 if ver1 > ver2
 */     
int versionCmp(char *ver1, char *ver2)
{
    char*  dot1;
    char*  dot2;
    int    num1;
    int    num2;

    dot1 = strchr(ver1, '.');
    dot2 = strchr(ver2, '.');

    num1 = atoi(ver1);
    num2 = atoi(ver2);

    if (num1 > num2)
    	return 1;
    	
	if (num1 < num2)
		return -1;
	
	if (dot1 && !dot2)   /* x.y > x */
        return 1;

    if (!dot1 && dot2)   /* x < x.y */
        return -1;
    
    if (!dot1 && !dot2)  /* x == x */
        return 0;

    return versionCmp((char*)(dot1 + 1), (char*)(dot2 + 1) );
}
