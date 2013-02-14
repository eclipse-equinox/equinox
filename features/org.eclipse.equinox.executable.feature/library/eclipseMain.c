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
 *******************************************************************************/
 
#include "eclipseUnicode.h"
#include "eclipseCommon.h"
#include "eclipseConfig.h"

#ifdef _WIN32
#include <direct.h>
#else
#include <unistd.h>
#endif
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <locale.h>
#include <sys/stat.h>

static _TCHAR* libraryMsg =
_T_ECLIPSE("The %s executable launcher was unable to locate its \n\
companion shared library.");

static _TCHAR* entryMsg =
_T_ECLIPSE("There was a problem loading the shared library and \n\
finding the entry point.");

#define NAME         _T_ECLIPSE("-name")
#define VMARGS       _T_ECLIPSE("-vmargs")		/* special option processing required */
/* New arguments have the form --launcher.<arg> to avoid collisions */
#define LIBRARY		  _T_ECLIPSE("--launcher.library")
#define SUPRESSERRORS _T_ECLIPSE("--launcher.suppressErrors")
#define INI			  _T_ECLIPSE("--launcher.ini")

/* this typedef must match the run method in eclipse.c */
typedef int (*RunMethod)(int argc, _TCHAR* argv[], _TCHAR* vmArgs[]);
typedef void (*SetInitialArgs)(int argc, _TCHAR*argv[], _TCHAR* library);

static _TCHAR*  name          = NULL;			/* program name */
static _TCHAR** userVMarg     = NULL;     		/* user specific args for the Java VM  */
static _TCHAR*  programDir	  = NULL;			/* directory where program resides */
static _TCHAR*  officialName  = NULL;
static int      suppressErrors = 0;				/* supress error dialogs */

static int 	 	createUserArgs(int configArgc, _TCHAR **configArgv, int *argc, _TCHAR ***argv);
static void  	parseArgs( int* argc, _TCHAR* argv[] );
static _TCHAR* 	getDefaultOfficialName(_TCHAR* program);
static _TCHAR*  findProgram(_TCHAR* argv[]);
static _TCHAR*  findLibrary(_TCHAR* library, _TCHAR* program);
static _TCHAR*  checkForIni(int argc, _TCHAR* argv[]);
static _TCHAR*  getDirFromProgram(_TCHAR* program);
 
static int initialArgc;
static _TCHAR** initialArgv;

_TCHAR* eclipseLibrary = NULL; /* path to the eclipse shared library */

#ifdef UNICODE
extern int main(int, char**);
int mainW(int, wchar_t**);
int wmain( int argc, wchar_t** argv ) {
	return mainW(argc, argv);
}

int main(int argc, char* argv[]) {
	/*
	* Run the UNICODE version, convert the arguments from MBCS to UNICODE
	*/
	int i, result;
	wchar_t **newArgv = malloc((argc + 1) * sizeof(wchar_t *));
	for (i=0; i<argc; i++) {
		char *oldArg = argv[i];
		int numChars = MultiByteToWideChar(CP_ACP, 0, oldArg, -1, NULL, 0);
		wchar_t *newArg  = malloc((numChars + 1) * sizeof(wchar_t));
		newArg[numChars] = 0;
		MultiByteToWideChar(CP_ACP, 0, oldArg, -1, newArg, numChars);
		newArgv[i] = newArg;
	}
	newArgv[i] = NULL;
	result = mainW(argc, newArgv);
	for (i=0; i<argc; i++) {
		free(newArgv[i]);
	}
	free(newArgv);
	return result;
}

#define main mainW
#endif /* UNICODE */

int main( int argc, _TCHAR* argv[] )
{
	_TCHAR*  errorMsg;
	_TCHAR*  program;
	_TCHAR*  iniFile;
	_TCHAR*  ch;
	_TCHAR** configArgv = NULL;
	int 	 configArgc = 0;
	int      exitCode = 0;
	int      ret = 0;
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
	 program = findProgram(argv);
    
    /* Parse configuration file arguments */
    iniFile = checkForIni(argc, argv);
    if (iniFile != NULL)
    	ret = readConfigFile(iniFile, &configArgc, &configArgv);
    else
    	ret = readIniFile(program, &configArgc, &configArgv);
	if (ret == 0)
	{
		parseArgs (&configArgc, configArgv);
	}
	
	/* Parse command line arguments           */
    /* Overrides configuration file arguments */
    parseArgs( &argc, argv );
    
    /* Special case - user arguments specified in the config file
	 * are appended to the user arguments passed from the command line.
	 */
	if (configArgc > 0)
	{	
		createUserArgs(configArgc, configArgv, &argc, &argv);
	}
	
	/* Initialize official program name */
	officialName = name != NULL ? _tcsdup( name ) : getDefaultOfficialName(program);
	
	/* Find the directory where the Eclipse program is installed. */
    programDir = getDirFromProgram(program);

	/* Find the eclipse library */
    eclipseLibrary = findLibrary(eclipseLibrary, program);
		
	if(eclipseLibrary != NULL)
		handle = loadLibrary(eclipseLibrary);
	if(handle == NULL) {
		errorMsg = malloc( (_tcslen(libraryMsg) + _tcslen(officialName) + 10) * sizeof(_TCHAR) );
        _stprintf( errorMsg, libraryMsg, officialName );
        if (!suppressErrors)
        	displayMessage( officialName, errorMsg );
        else
        	_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, errorMsg);
        free( errorMsg );
    	exit( 1 );
	}

	setArgs = (SetInitialArgs)findSymbol(handle, SET_INITIAL_ARGS);
	if(setArgs != NULL)
		setArgs(initialArgc, initialArgv, eclipseLibrary);
	else {
		if(!suppressErrors)
			displayMessage(officialName, entryMsg);
		else 
			_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, entryMsg);
		exit(1);
	}
	
	runMethod = (RunMethod)findSymbol(handle, RUN_METHOD);
	if(runMethod != NULL)
		exitCode = runMethod(argc, argv, userVMarg);
	else { 
		if(!suppressErrors)
			displayMessage(officialName, entryMsg);
		else 
			_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, entryMsg);
		exit(1);
	}
	unloadLibrary(handle);
	
	free( eclipseLibrary );
    free( programDir );
    free( program );
    free( officialName );
    
	return exitCode;
}

_TCHAR* getProgramPath() {
	return NULL;
}

static _TCHAR* findProgram(_TCHAR* argv[]) {
	_TCHAR * program;
#ifdef _WIN32
	 /* windows, make sure we are looking for the .exe */
	_TCHAR * ch;
	int length = _tcslen(argv[0]);
	ch = malloc( (length + 5) * sizeof(_TCHAR));
	_tcscpy(ch, argv[0]);
	 
	if (length <= 4 || _tcsicmp( &ch[ length - 4 ], _T_ECLIPSE(".exe") ) != 0)
		_tcscat(ch, _T_ECLIPSE(".exe"));
	
	program = findCommand(ch);
	if (ch != program)
		free(ch);
#else
	program = findCommand( argv[0] );
#endif
    if (program == NULL)
    {
#ifdef _WIN32
    	program = malloc( MAX_PATH_LENGTH + 1 );
    	GetModuleFileName( NULL, program, MAX_PATH_LENGTH );
    	argv[0] = program;
#else
    	program = malloc( (strlen( argv[0] ) + 1) * sizeof(_TCHAR) );
    	strcpy( program, argv[0] );
#endif
    } else if (_tcscmp(argv[0], program) != 0) {
    	argv[0] = program;
    }
    return program;
}

/*
 * Parse arguments of the command.
 */
static void parseArgs( int* pArgc, _TCHAR* argv[] )
{
    int     index;

    /* Ensure the list of user argument is NULL terminated. */
    argv[ *pArgc ] = NULL;

	/* For each user defined argument */
    for (index = 0; index < *pArgc; index++){
        if(_tcsicmp(argv[index], VMARGS) == 0) {
        	userVMarg = &argv[ index+1 ];
            argv[ index ] = NULL;
            *pArgc = index;
        } else if(_tcsicmp(argv[index], NAME) == 0) {
        	name = argv[++index];
        } else if(_tcsicmp(argv[index], LIBRARY) == 0) {
        	eclipseLibrary = argv[++index];
        } else if(_tcsicmp(argv[index], SUPRESSERRORS) == 0) {
        	suppressErrors = 1;
        } 
    }
}

/* We need to look for --launcher.ini before parsing the other args */
static _TCHAR* checkForIni(int argc, _TCHAR* argv[]) 
{
	int index;
	for(index = 0; index < (argc - 1); index++) {
		if(_tcsicmp(argv[index], INI) == 0) {
        	return argv[++index];
        } 
	}
	return NULL;
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
	_TCHAR** newArray = (_TCHAR **)malloc((configArgc + *argc + 1) * sizeof(_TCHAR *));

	newArray[0] = (*argv)[0];	/* use the original argv[0] */
	memcpy(newArray + 1, configArgv, configArgc * sizeof(_TCHAR *));	
	
	/* Skip the argument zero (program path and name) */
	memcpy(newArray + 1 + configArgc, *argv + 1, (*argc - 1) * sizeof(_TCHAR *));

	/* Null terminate the new list of arguments and return it. */	 
	*argv = newArray;
	*argc += configArgc;
	(*argv)[*argc] = NULL;
	
	return 0;
}

/* Determine the Program Directory
 *
 * This function takes the directory where program executable resides and
 * determines the installation directory.
 */
_TCHAR* getDirFromProgram(_TCHAR* program)
{
	_TCHAR*  ch;
	
	if(programDir != NULL)
		return programDir;

    programDir = malloc( (_tcslen( program ) + 1) * sizeof(_TCHAR) );
    _tcscpy( programDir, program );
    ch = lastDirSeparator( programDir );
	if (ch != NULL)
    {
    	*(ch+1) = _T_ECLIPSE('\0');
   		return programDir;
    }

	/* Can't figure out from the program, lets use the cwd */
	free(programDir);
	programDir = malloc( MAX_PATH_LENGTH * sizeof (_TCHAR));
	_tgetcwd( programDir, MAX_PATH_LENGTH );
	return programDir;
}

_TCHAR* getProgramDir()
{
	return programDir;
}

_TCHAR* getOfficialName() {
	return officialName;
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
	ch = lastDirSeparator( program );
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

static _TCHAR* findLibrary(_TCHAR* library, _TCHAR* program) 
{
	_TCHAR* c;
	_TCHAR* path;
	_TCHAR* fragment;
	_TCHAR* result;
	_TCHAR* dot = _T_ECLIPSE(".");
	size_t progLength, pathLength;
	size_t fragmentLength;
	struct _stat stats;
	
	if (library != NULL) {
		path = checkPath(library, programDir, 1);
		if (_tstat(path, &stats) == 0 && (stats.st_mode & S_IFDIR) != 0) 
        {
            /* directory, find the highest version eclipse_* library */
            result = findFile(path, _T_ECLIPSE("eclipse"));
        } else {
        	/* file, return it */
        	result = _tcsdup(path);
        }

		if (path != library)
			free(path);
		return result;
	}
	
	/* build the equinox.launcher fragment name */
	fragmentLength = _tcslen(DEFAULT_EQUINOX_STARTUP) + 1 + _tcslen(wsArg) + 1 + _tcslen(osArg) + 1 + _tcslen(osArchArg) + 1;
	fragment = malloc(fragmentLength * sizeof(_TCHAR));
	_tcscpy(fragment, DEFAULT_EQUINOX_STARTUP);
	_tcscat(fragment, dot);
	_tcscat(fragment, wsArg);
	_tcscat(fragment, dot);
	_tcscat(fragment, osArg);
	//!(fragmentOS.equals(Constants.OS_MACOSX) && !Constants.ARCH_X86_64.equals(fragmentArch))
#if !(defined(MACOSX) && !defined(__x86_64__)) 
	/* The Mac fragment covers both archs and does not have that last segment */
	_tcscat(fragment, dot);
	_tcscat(fragment, osArchArg);
#endif	
	progLength = pathLength = _tcslen(programDir);
#ifdef MACOSX
	pathLength += 9;
#endif
	path = malloc( (pathLength + 1 + 7 + 1) * sizeof(_TCHAR));
	_tcscpy(path, programDir);
	if (!IS_DIR_SEPARATOR(path[progLength - 1])) {
		path[progLength] = dirSeparator;
		path[progLength + 1] = 0;
	}
#ifdef MACOSX
	_tcscat(path, _T_ECLIPSE("../../../"));
#endif
	_tcscat(path, _T_ECLIPSE("plugins"));
	
	c = findFile(path, fragment);
	free(fragment);
	if (c == NULL)
		return c;
	fragment = c;
	
	result = findFile(fragment, _T_ECLIPSE("eclipse"));
	
	free(fragment);
	free(path);
	
	return result; 
}
