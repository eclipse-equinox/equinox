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
 
#include "eclipseUnicode.h"
#include "eclipseCommon.h"
#include "eclipseConfig.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <locale.h>
#include <sys/stat.h>

static _TCHAR* libraryMsg =
_T_ECLIPSE("The %s executable launcher was unable to locate its \n\
companion shared library.");

#define NAME         _T_ECLIPSE("-name")
#define LIBRARY		 _T_ECLIPSE("-library")
#define VMARGS       _T_ECLIPSE("-vmargs")		/* special option processing required */

/* this typedef must match the run method in eclipse.c */
typedef int (*RunMethod)(int argc, _TCHAR* argv[], _TCHAR* vmArgs[]);
typedef void (*SetInitialArgs)(int argc, _TCHAR*argv[]);

static _TCHAR*  name          = NULL;			/* program name */
static _TCHAR** userVMarg     = NULL;     		/* user specific args for the Java VM  */
static _TCHAR*  programDir	  = NULL;			/* directory where program resides */
static _TCHAR*  library		  = NULL;			/* pathname of the eclipse shared library */

static int 	 	createUserArgs(int configArgc, _TCHAR **configArgv, int *argc, _TCHAR ***argv);
static void  	parseArgs( int* argc, _TCHAR* argv[] );
static _TCHAR* 	getDefaultOfficialName(_TCHAR* program);
static _TCHAR*  findLibrary(_TCHAR* program);
 
static int initialArgc;
static _TCHAR** initialArgv;

#ifdef _WIN32
#ifdef UNICODE
extern int main(int, char**);
int mainW(int, wchar_t**);
int wmain( int argc, wchar_t** argv ) {
	OSVERSIONINFOW info;
	info.dwOSVersionInfoSize = sizeof(OSVERSIONINFOW);
	/*
	* If the OS supports UNICODE functions, run the UNICODE version
	* of the main function. Otherwise, convert the arguments to
	* MBCS and run the ANSI version of the main function.
	*/
	if (!GetVersionExW (&info)) {
		int i, result;
		char **newArgv = malloc(argc * sizeof(char *));
		for (i=0; i<argc; i++) {
			wchar_t *oldArg = argv[i];
			int byteCount = WideCharToMultiByte (CP_ACP, 0, oldArg, -1, NULL, 0, NULL, NULL);
			char *newArg  = malloc(byteCount+1);
			newArg[byteCount] = 0;
			WideCharToMultiByte (CP_ACP, 0, oldArg, -1, newArg, byteCount, NULL, NULL);
			newArgv[i] = newArg;
		}
		result = main(argc, newArgv);
		for (i=0; i<argc; i++) {
			free(newArgv[i]);
		}
		free(newArgv);
		return result;
	}
	return mainW(argc, argv);
}
#define main mainW
#endif /* UNICODE */
#endif /* _WIN32 */

int main( int argc, _TCHAR* argv[] )
{
	_TCHAR*  errorMsg;
	_TCHAR*  program;
	_TCHAR*  ch;
	_TCHAR** configArgv = NULL;
	int 	 configArgc = 0;
	int      exitCode = 0;
	void *	 handle = 0;
	RunMethod 		runMethod;
	SetInitialArgs  setArgs;
	
	setlocale(LC_ALL, "");
	
	initialArgc = argc;
	initialArgv = malloc((argc + 1) * sizeof(_TCHAR*));
	memcpy(initialArgv, argv, (argc + 1) * sizeof(_TCHAR*));
	
	/* 
	 * Strip off any extroneous <CR> from the last argument. If a shell script
	 * on Linux is created in DOS format (lines end with <CR><LF>), the C-shell
	 * does not strip off the <CR> and hence the argument is bogus and may 
	 * not be recognized by the launcher or eclipse itself.
	 */
	 ch = _tcschr( argv[ argc - 1 ], _T_ECLIPSE('\r') );
	 if (ch != NULL)
	 {
	     *ch = _T_ECLIPSE('\0');
	 }
	 
	 /* Determine the full pathname of this program. */
    program = findCommand( argv[0] );
    if (program == NULL)
    {
#ifdef _WIN32
    	program = malloc( MAX_PATH_LENGTH + 1 );
    	GetModuleFileName( NULL, program, MAX_PATH_LENGTH );
#else
    	program = malloc( strlen( argv[0] ) + 1 );
    	strcpy( program, argv[0] );
#endif
    }
    
    /* Parse configuration file arguments */
	if (readConfigFile(program, argv[0], &configArgc, &configArgv) == 0)
	{
		parseArgs (&configArgc, configArgv);
	}
	
	/* Parse command line arguments           */
    /* Overrides configuration file arguments */
    parseArgs( &argc, argv );
    
    /* Special case - user arguments specified in the config file
	 * are appended to the user arguments passed from the command line.
	 */
	if (configArgc > 1)
	{	
		createUserArgs(configArgc, configArgv, &argc, &argv);
	}
	
	/* Initialize official program name */
	officialName = name != NULL ? _tcsdup( name ) : getDefaultOfficialName(program);
	
	/* Find the directory where the Eclipse program is installed. */
    programDir = getProgramDir(program);

	/* Find the eclipse library */
	if(library == NULL) {
		library = findLibrary(program);
	}
	if(library != NULL)
		handle = loadLibrary(library);
	if(handle == NULL) {
		errorMsg = malloc( (_tcslen(libraryMsg) + _tcslen(officialName) + 10) * sizeof(_TCHAR) );
        _stprintf( errorMsg, libraryMsg, officialName );
        displayMessage( officialName, errorMsg );
        free( errorMsg );
    	exit( 1 );
	}

	setArgs = findSymbol(handle, SET_INITIAL_ARGS);
	if(setArgs != NULL)
		setArgs(initialArgc, initialArgv);
	
	runMethod = findSymbol(handle, RUN_METHOD);
	exitCode = runMethod(argc, argv, userVMarg);
	unloadLibrary(handle);
	
	free( library );
    free( programDir );
    free( officialName );
    
	return exitCode;
}

/*
 * Parse arguments of the command.
 */
static void parseArgs( int* pArgc, _TCHAR* argv[] )
{
    int     index;

    /* Ensure the list of user argument is NULL terminated. */
    argv[ *pArgc ] = NULL;

	/* For each user defined argument (excluding the program) */
    for (index = 1; index < *pArgc; index++){
        if(_tcsicmp(argv[index], VMARGS) == 0) {
        	userVMarg = &argv[ index+1 ];
            argv[ index ] = NULL;
            *pArgc = index;
        } else if(_tcsicmp(argv[index], NAME) == 0) {
        	name = argv[++index];
        } else if(_tcsicmp(argv[index], LIBRARY) == 0) {
        	library = argv[++index];
        } 
    }
}

/*
 * Create a new array containing user arguments from the config file first and
 * from the command line second.
 * Allocate an array large enough to host all the strings passed in from
 * the argument configArgv and argv. That array is passed back to the
 * argv argument. That array must be freed with the regular free().
 * Note that both arg lists are expected to contain the argument 0 from the C
 * main method. That argument contains the path/executable name. It is
 * only copied once in the resulting list.
 *
 * Returns 0 if success.
 */
static int createUserArgs(int configArgc, _TCHAR **configArgv, int *argc, _TCHAR ***argv)
{
	 _TCHAR** newArray = (_TCHAR **)malloc((configArgc + *argc) * sizeof(_TCHAR *));

	memcpy(newArray, configArgv, configArgc * sizeof(_TCHAR *));	
	
	/* Skip the argument zero (program path and name) */
	memcpy(newArray + configArgc, *argv + 1, (*argc - 1) * sizeof(_TCHAR *));

	/* Null terminate the new list of arguments and return it. */	 
	*argv = newArray;
	*argc += configArgc - 1;
	(*argv)[*argc] = NULL;
	
	return 0;
}

/* Determine the Program Directory
 *
 * This function takes the directory where program executable resides and
 * determines the installation directory.
 */
_TCHAR* getProgramDir(_TCHAR* program)
{
	if(programDir != NULL)
		return programDir;
	_TCHAR*  ch;

    programDir = malloc( (_tcslen( program ) + 1) * sizeof(_TCHAR) );
    _tcscpy( programDir, program );
    ch = _tcsrchr( programDir, dirSeparator );
	if (ch != NULL)
    {
    	*(ch+1) = _T_ECLIPSE('\0');
   		return programDir;
    }

	/* Can't figure out from the program */
	free(programDir);
	programDir = NULL;
	return NULL;
}

/*
 * Determine the default official application name
 *
 * This function provides the default application name that appears in a variety of
 * places such as: title of message dialog, title of splash screen window
 * that shows up in Windows task bar.
 * It is computed from the name of the launcher executable and
 * by capitalizing the first letter. e.g. "c:/ide/eclipse.exe" provides
 * a default name of "Eclipse".
 */
static _TCHAR* getDefaultOfficialName(_TCHAR* program)
{
	_TCHAR *ch = NULL;
	
	/* Skip the directory part */
	ch = _tcsrchr( program, dirSeparator );
	if (ch == NULL) ch = program;
	else ch++;
	
	ch = _tcsdup( ch );
#ifdef _WIN32
	{
		/* Search for the extension .exe and cut it */
		_TCHAR *extension = _tcsrchr(ch, _T_ECLIPSE('.'));
		if (extension != NULL) 
		{
			*extension = _T_ECLIPSE('\0');
		}
	}
#endif
	/* Upper case the first character */
#ifndef LINUX
	{
		*ch = _totupper(*ch);
	}
#else
	{
		if (*ch >= 'a' && *ch <= 'z')
		{
			*ch -= 32;
		}
	}
#endif
	return ch;
}

static _TCHAR* findLibrary(_TCHAR* program) 
{
	_TCHAR* c;
	_TCHAR* libraryPrefix;
	_TCHAR* path;
	_TCHAR* result;
	int length;
	
	/* find the last segment */
	c = _tcsrchr(program, dirSeparator);
	if(c == NULL)
		libraryPrefix = _tcsdup(program);
	else
		libraryPrefix = _tcsdup(++c); /* next character is start of prefix */

#ifdef _WIN32
	{
		/* Search for the extension .exe and remove it */
		_TCHAR *extension = _tcsrchr(libraryPrefix, _T_ECLIPSE('.'));
		if (extension == NULL || _tcslen(extension) < 4)
		{
			free(libraryPrefix);
			return NULL;
		}
		extension[0] = 0;
	}
#endif
	
	length = _tcslen(programDir);
	if(programDir[length - 1] == dirSeparator){
		path = _tcsdup(programDir);
		path[length - 1] = 0;
	}  else {
		path = programDir;
	}
	result = findFile(path, libraryPrefix);
	
	free(libraryPrefix);
	if(path != programDir)
		free(path);
	
	return result; 
}
