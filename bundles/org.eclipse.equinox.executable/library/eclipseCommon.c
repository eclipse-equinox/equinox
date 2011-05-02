/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Niefer
 * Martin Oberhuber (Wind River) - [176805] Support Solaris9 by adding setenv()
 *******************************************************************************/
 
#include "eclipseCommon.h"
#include "eclipseUnicode.h"

#ifdef _WIN32
#include <direct.h>
#include <windows.h>
#else
#include <unistd.h>
#include <string.h>
#include <dirent.h>
#include <limits.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <errno.h>

/* Global Variables */
_TCHAR* osArg        = _T_ECLIPSE(DEFAULT_OS);
#ifdef MACOSX
	/* on the mac we have a universal binary, decide ppc vs x86 based on endianness */
	#ifdef __BIG_ENDIAN__
		_TCHAR* osArchArg    = _T_ECLIPSE("ppc");
	#else
		_TCHAR* osArchArg    = _T_ECLIPSE(DEFAULT_OS_ARCH);
	#endif
#else
_TCHAR* osArchArg    = _T_ECLIPSE(DEFAULT_OS_ARCH);
#endif
_TCHAR* wsArg        = _T_ECLIPSE(DEFAULT_WS);	/* the SWT supported GUI to be used */

/* Local Variables */
static _TCHAR* filterPrefix = NULL;  /* prefix for the find files filter */
static size_t  prefixLength = 0;

static int isFolder(const _TCHAR* path, const _TCHAR* entry);

typedef struct {
	int segment[3];
	_TCHAR * qualifier;
} Version;

static void freeVersion(Version *version)
{
	if(version->qualifier)
		free(version->qualifier);
	free(version);
}

static Version* parseVersion(const _TCHAR * str) {
	_TCHAR *copy;
	_TCHAR *c1, *c2 = NULL;
	int i = 0;
	
	Version *version = malloc(sizeof(Version));
	memset(version, 0, sizeof(Version));
	
	c1 = copy = _tcsdup(str);
	while (c1 && *c1 != 0)
	{
		if (i < 3) {
			version->segment[i] = (int)_tcstol(c1, &c2, 10);
			/* if the next character is not '.', then we couldn't
			 * parse as a int, the remainder is not valid (or we are at the end)*/
			if (*c2 && *c2 != _T_ECLIPSE('.'))
				break;
			c2++; /* increment past the . */
		} else {
			c2 = _tcschr(c1, _T_ECLIPSE('.'));
			if(c2 != NULL) {
				*c2 = 0;
				version->qualifier = _tcsdup(c1);
				*c2 = _T_ECLIPSE('.'); /* put the dot back */
			} else {
				if(_tcsicmp(c1, _T_ECLIPSE("jar")) == 0)
					version->qualifier = 0;
				else
					version->qualifier = _tcsdup(c1);
			}
			break;
		}
		c1 = c2;
		i++;
	}
	free(copy);
	return version;
}

static int compareVersions(const _TCHAR* str1, const _TCHAR* str2) {
	int result = 0, i = 0;
	Version *v1 = parseVersion(str1);
	Version *v2 = parseVersion(str2);
	
	while (result == 0 && i < 3) {
		result = v1->segment[i] - v2->segment[i];
		i++;
	}
	if(result == 0) {
		_TCHAR * q1 = v1->qualifier ? v1->qualifier : _T_ECLIPSE("");
		_TCHAR * q2 = v2->qualifier ? v2->qualifier : _T_ECLIPSE("");
		result =  _tcscmp(q1, q2);
	}
	
	freeVersion(v1);
	freeVersion(v2);
	return result;
}

/**
 * Convert a wide string to a narrow one
 * Caller must free the null terminated string returned.
 */
char *toNarrow(const _TCHAR* src)
{
#ifdef UNICODE
	int byteCount = WideCharToMultiByte (CP_ACP, 0, (wchar_t *)src, -1, NULL, 0, NULL, NULL);
	char *dest = malloc(byteCount+1);
	dest[byteCount] = 0;
	WideCharToMultiByte (CP_ACP, 0, (wchar_t *)src, -1, dest, byteCount, NULL, NULL);
	return dest;
#else
	return (char*)_tcsdup(src);
#endif
}


/**
 * Set an environment variable.
 * Solaris versions <= Solaris 9 did not know setenv in libc,
 * so emulate it here.
 */
#if defined(SOLARIS) || defined(HPUX)
int setenv (const char *name, const char *value, int replace)
{
	int namelen, valuelen, rc;
	char *var;
	if (replace == 0) {
		const char *oldval = getenv(name);
		if (oldval != NULL) {
			return 0;
	    }
	}
	namelen = strlen(name);
	valuelen = strlen(value);
	var = malloc( (namelen + valuelen + 2) * sizeof(char) );
	if (var == NULL) {
		return -1;
	}
	/* Use strncpy as protection, in case a thread modifies var
	 * after we obtained its length */
	strncpy(var, name, namelen);
	var[namelen] = '=';
	strncpy( &var[namelen + 1], value, valuelen);
	var[namelen + valuelen + 1] = '\0';
	rc = putenv(var);
	if (rc != 0) rc = -1; /*putenv returns non-zero on error; setenv -1*/
	return rc;
}
#endif
 	
 /*
 * Find the absolute pathname to where a command resides.
 *
 * The string returned by the function must be freed.
 */
#define EXTRA 20
_TCHAR* findCommand( _TCHAR* command )
{
	return findSymlinkCommand( command, 1 );
}

_TCHAR* findSymlinkCommand( _TCHAR* command, int resolve )
{
    _TCHAR*  cmdPath;
    size_t   length;
    _TCHAR*  ch;
    _TCHAR*  dir;
    _TCHAR*  path;
    struct _stat stats;

    /* If the command was an abolute pathname, use it as is. */
    if (IS_ABSOLUTE(command))
    {
        length = _tcslen( command );
        cmdPath = malloc( (length + EXTRA) * sizeof(_TCHAR) ); /* add extra space for a possible ".exe" extension */
        _tcscpy( cmdPath, command );
    }

    else
    {
        /* If the command string contains a path separator */
        if (firstDirSeparator( command ) != NULL)
        {
            /* It must be relative to the current directory. */
            length = MAX_PATH_LENGTH + EXTRA + _tcslen( command );
            cmdPath = malloc( length * sizeof (_TCHAR));
            _tgetcwd( cmdPath, length );
            length = _tcslen(cmdPath);
            if (!IS_DIR_SEPARATOR(cmdPath[ length - 1 ]))
            {
                cmdPath[ length ] = dirSeparator;
                cmdPath[ length+1 ] = _T_ECLIPSE('\0');
            }
            _tcscat( cmdPath, command );
        }

        /* else the command must be in the PATH somewhere */
        else
        {
            /* Get the directory PATH where executables reside. */
            path = _tgetenv( _T_ECLIPSE("PATH") );
#ifdef _WIN32
            /* on windows, prepend the current directory */
            if (path == NULL)
            	path = _T_ECLIPSE("");
            ch = malloc((_tcslen(path) + MAX_PATH_LENGTH + 2) * sizeof(_TCHAR));
            _tgetcwd( ch, MAX_PATH_LENGTH );
            length = _tcslen(ch);
            ch[length] = pathSeparator;
            _tcscpy(&ch[length + 1], path);
            path = ch;
#endif
            if (!path)
            {
	            return NULL;
            }
            else
            {
	            length = _tcslen( path ) + _tcslen( command ) + MAX_PATH_LENGTH;
	            cmdPath = malloc( length * sizeof(_TCHAR));
	
	            /* Foreach directory in the PATH */
	            dir = path;
	            while (dir != NULL && *dir != _T_ECLIPSE('\0'))
	            {
	                ch = _tcschr( dir, pathSeparator );
	                if (ch == NULL)
	                {
	                    _tcscpy( cmdPath, dir );
	                }
	                else
	                {
	                    length = ch - dir;
	                    _tcsncpy( cmdPath, dir, length );
	                    cmdPath[ length ] = _T_ECLIPSE('\0');
	                    ch++;
	                }
	                dir = ch; /* advance for the next iteration */

#ifdef _WIN32
                    /* Remove quotes */
	                if (_tcschr( cmdPath, _T_ECLIPSE('"') ) != NULL)
	                {
	                    size_t i = 0, j = 0;
	                    _TCHAR c;
	                    length = _tcslen( cmdPath );
	                    while (i < length) {
	                        c = cmdPath[ i++ ];
	                        if (c == _T_ECLIPSE('"')) continue;
	                        cmdPath[ j++ ] = c;
	                    }
	                    cmdPath[ j ] = _T_ECLIPSE('\0');
	                }
#endif
	                /* Determine if the executable resides in this directory. */
	                if (_tcslen(cmdPath) == 0 || /*an empty path entry is treated as '.' */
	                	(cmdPath[0] == _T_ECLIPSE('.') && (_tcslen(cmdPath) == 1 || (_tcslen(cmdPath) == 2 && IS_DIR_SEPARATOR(cmdPath[1])))))
	                {
	                	_tgetcwd( cmdPath, MAX_PATH_LENGTH );
	                }
	                length = _tcslen(cmdPath);
	                if (!IS_DIR_SEPARATOR(cmdPath[ length - 1 ]))
	                {
	                    cmdPath[ length ] = dirSeparator;
	                    cmdPath[ length+1 ] = _T_ECLIPSE('\0');
	                }
	                _tcscat( cmdPath, command );
	
	                /* If the file is not a directory and can be executed */
	                if (_tstat( cmdPath, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
	                {
	                    /* Stop searching */
	                    dir = NULL;
	                }
	            }
	        }
        }
    }

#ifdef _WIN32
	/* If the command does not exist */
    if (_tstat( cmdPath, &stats ) != 0 || (stats.st_mode & S_IFREG) == 0)
    {
    	/* If the command does not end with .exe, append it an try again. */
    	length = _tcslen( cmdPath );
    	if (length > 4 && _tcsicmp( &cmdPath[ length - 4 ], _T_ECLIPSE(".exe") ) != 0)
    	    _tcscat( cmdPath, _T_ECLIPSE(".exe") );
    }
#endif

    /* Verify the resulting command actually exists. */
    if (_tstat( cmdPath, &stats ) != 0 || (stats.st_mode & S_IFREG) == 0)
    {
        free( cmdPath );
        cmdPath = NULL;
        return cmdPath;
    }

	if (resolve) {
		ch = resolveSymlinks(cmdPath);
		if (ch != cmdPath) {
			free(cmdPath);
			cmdPath = ch;
		}
	}
	return cmdPath;
}

#if !defined(_WIN32) && !defined(MACOSX)
char * resolveSymlinks( char * path ) {
	char * ch, *buffer;
	if(path == NULL)
		return path;
	/* resolve symlinks */
	ch = path;
	buffer = malloc(PATH_MAX);
    path = realpath(path, buffer);
    if (path != buffer)
    	free(buffer);
    if (path == NULL)
    	return ch; /* failed to resolve the links, return original path */
    return path;
}
#endif

#ifdef _WIN32
static int filter(_TCHAR* candidate, int isFolder) {
#else
#ifdef MACOSX
static int filter(struct dirent *dir, int isFolder) {
#else
static int filter(const struct dirent *dir, int isFolder) {
#endif
	char * candidate = (char *)dir->d_name;
#endif
	_TCHAR *lastDot, *lastUnderscore;
	int result;
	
	if(_tcslen(candidate) <= prefixLength)
		return 0;
	if (_tcsncmp(candidate, filterPrefix, prefixLength) != 0 ||	candidate[prefixLength] != _T_ECLIPSE('_'))
		return 0;
	
	candidate = _tcsdup(candidate);
	
	/* remove trailing .jar and .zip extensions, leave other extensions because we need the '.'  */
	lastDot = _tcsrchr(candidate, _T_ECLIPSE('.'));
	if (!isFolder && lastDot != NULL && (_tcscmp(lastDot, _T_ECLIPSE(".jar")) == 0 || _tcscmp(lastDot, _T_ECLIPSE(".zip")) == 0)) {
		*lastDot = 0;
		lastDot = _tcsrchr(candidate, _T_ECLIPSE('.'));
	}
	
	if (lastDot < &candidate[prefixLength]) {
		free(candidate);
		return 0;
	}
	
	lastUnderscore = _tcsrchr(candidate, _T_ECLIPSE('_'));
	
	/* get past all the '_' that are part of the qualifier */
	while(lastUnderscore > lastDot) {
		*lastUnderscore = 0;
		lastUnderscore = _tcsrchr(candidate, _T_ECLIPSE('_')); 
	}
	/* is this the underscore at the end of the prefix? */
	result = (lastUnderscore == &candidate[prefixLength]);
	free(candidate);
	return result;
}

 /* 
 * Looks for files of the form /path/prefix_version.<extension> and returns the full path to
 * the file with the largest version number
 */ 
_TCHAR* findFile( _TCHAR* path, _TCHAR* prefix)
{
	struct _stat stats;
	size_t pathLength;
	_TCHAR* candidate = NULL;
	_TCHAR* result = NULL;
	
#ifdef _WIN32
	_TCHAR* fileName = NULL;
	WIN32_FIND_DATA data;
	HANDLE handle;
#else	
	DIR *dir = NULL;
	struct dirent * entry = NULL;
#endif
	
	path = _tcsdup(path);
	pathLength = _tcslen(path);
	
	/* strip dirSeparators off the end */
	while (IS_DIR_SEPARATOR(path[pathLength - 1])) {
		path[--pathLength] = 0;
	}
	
	/* does path exist? */
	if( _tstat(path, &stats) != 0 ) {
		free(path);
		return NULL;
	}
	
	filterPrefix = prefix;
	prefixLength = _tcslen(prefix);
#ifdef _WIN32
	fileName = malloc( (_tcslen(path) + 1 + _tcslen(prefix) + 3) * sizeof(_TCHAR));
	_stprintf(fileName, _T_ECLIPSE("%s%c%s_*"), path, dirSeparator, prefix);
	
	handle = FindFirstFile(fileName, &data);
	if(handle != INVALID_HANDLE_VALUE) {
		if (filter(data.cFileName, isFolder(path, data.cFileName)))
			candidate = _tcsdup(data.cFileName);
		while(FindNextFile(handle, &data) != 0) {
			if (filter(data.cFileName, isFolder(path, data.cFileName))) {
				if (candidate == NULL) {
					candidate = _tcsdup(data.cFileName);
				} else if( compareVersions(candidate + prefixLength + 1, data.cFileName + prefixLength + 1) < 0) {
					/* compare, take the highest version */
					free(candidate);
					candidate = _tcsdup(data.cFileName);
				}
			}
		}
		FindClose(handle);
	}
#else
	if ((dir = opendir(path)) == NULL) {
		free(path);
		return NULL;
	}

	while ((entry = readdir(dir)) != NULL) {
		if (filter(entry, isFolder(path, entry->d_name))) {
			if (candidate == NULL) {
				candidate = _tcsdup(entry->d_name);
			} else if (compareVersions(candidate + prefixLength + 1, entry->d_name + prefixLength + 1) < 0) {
				free(candidate);
				candidate = _tcsdup(entry->d_name);
			}
		}
	}
	closedir(dir);
#endif

	if(candidate != NULL) {
		result = malloc((pathLength + 1 + _tcslen(candidate) + 1) * sizeof(_TCHAR));
		_tcscpy(result, path);
		result[pathLength] = dirSeparator;
		result[pathLength + 1] = 0;
		_tcscat(result, candidate);
		free(candidate);
	}
	free(path);
	return result;
}

int isFolder(const _TCHAR* path, const _TCHAR* entry) {
	int result = 0;
	struct _stat stats;
	_TCHAR * fullPath = malloc((_tcslen(path) + _tcslen(entry) + 2) * sizeof(_TCHAR));
	_stprintf(fullPath, _T_ECLIPSE("%s%c%s"), path, dirSeparator, entry);
	
	result = _tstat(fullPath, &stats);
	free(fullPath);
	return (result == 0 && (stats.st_mode & S_IFDIR) != 0);
}

/*
 * If path is relative, attempt to make it absolute by 
 * 1) check relative to working directory
 * 2) check relative to provided programDir
 * If reverseOrder, then check the programDir before the working dir
 */
_TCHAR* checkPath( _TCHAR* path, _TCHAR* programDir, int reverseOrder ) 
{
	int cwdLength = MAX_PATH_LENGTH;
	int i;
	_TCHAR * workingDir, * buffer, * result = NULL;
	_TCHAR * paths[2];
	struct _stat stats;
	
	/* If the command was an abolute pathname, use it as is. */
    if (IS_ABSOLUTE(path)) {
    	return path;
    }
    
    /* get the current working directory */
    workingDir = malloc(cwdLength * sizeof(_TCHAR));
    while ( _tgetcwd( workingDir, cwdLength ) == NULL ){
    	if (errno == ERANGE) {
    		/* ERANGE : the buffer isn't big enough, allocate more memory */
			cwdLength *= 2;
			workingDir = realloc(workingDir, cwdLength * sizeof(_TCHAR));
			continue;
    	} else {
    		/* some other error occurred, perhaps ENOENT (directory has been unlinked) */
    		/* the contents of workingDir are undefined, set it to empty, we will end up testing against root */
    		workingDir[0] = _T_ECLIPSE('\0');
    		break;
    	}
    }
    
    paths[0] = reverseOrder ? programDir : workingDir;
    paths[1] = reverseOrder ? workingDir : programDir;
    
    /* just make a buffer big enough to hold everything */
    buffer = malloc((_tcslen(paths[0]) + _tcslen(paths[1]) + _tcslen(path) + 2) * sizeof(_TCHAR));
    for ( i = 0; i < 2; i++ ) {
    	if (_tcslen(paths[i]) == 0)
    		continue;
    	_stprintf(buffer, _T_ECLIPSE("%s%c%s"), paths[i], dirSeparator, path);
    	if (_tstat(buffer, &stats) == 0) {
    		result = _tcsdup(buffer);
    		break;
    	}
    }
    
    free(buffer);
    free(workingDir);
    
    /* if we found something, return it, otherwise, return the original */
    return result != NULL ? result : path;
}

_TCHAR * lastDirSeparator(_TCHAR* str) {
#ifndef _WIN32
	return _tcsrchr(str, dirSeparator);
#else
	int i = -1;
	_TCHAR * c = NULL;
	while (str[++i] != 0) {
		if (str[i] == _T_ECLIPSE('\\') || str[i] == _T_ECLIPSE('/'))
			c = &str[i];
	}
	return c;
#endif
}

_TCHAR * firstDirSeparator(_TCHAR* str) {
#ifdef _WIN32
	return _tcspbrk(str, _T_ECLIPSE("\\/"));
#else
	return _tcschr(str, dirSeparator);
#endif
}
