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
#include <dirent.h>
#include <locale.h>
#include <sys/stat.h>

static _TCHAR* startupMsg =
_T_ECLIPSE("The %s executable launcher was unable to locate its \n\
companion startup jar file.");

static _TCHAR* libraryMsg =
_T_ECLIPSE("The %s executable launcher was unable to locate its \n\
companion shared library.");

#define DEFAULT_EQUINOX_STARTUP _T_ECLIPSE("org.eclipse.equinox.startup")
#define DEFAULT_STARTUP 		_T_ECLIPSE("startup.jar")

#define STARTUP      _T_ECLIPSE("-startup")
#define NAME         _T_ECLIPSE("-name")
#define VMARGS       _T_ECLIPSE("-vmargs")		/* special option processing required */

typedef int (*RunMethod)(int argc, _TCHAR* argv[]);

static _TCHAR*	startupArg    = NULL;			/* path of the startup.jar the user wants to run relative to the program path */
static _TCHAR*  name          = NULL;			/* program name */
static _TCHAR** userVMarg     = NULL;     		/* user specific args for the Java VM  */
static _TCHAR*  jarFile       = NULL;			/* full pathname of the startup jar file to run */
static _TCHAR*  programDir	  = NULL;			/* directory where program resides */
static _TCHAR*  library		  = NULL;			/* pathname of the eclipse shared library */

static Option options[] = {
    { STARTUP,		&startupArg,	NULL,	0 },
    { NAME,         &name,			NULL,	0 }};
static int optionsSize = (sizeof(options) / sizeof(options[0]));

static int 	 	createUserArgs(int configArgc, _TCHAR **configArgv, int *argc, _TCHAR ***argv);
static void  	parseArgs( int* argc, _TCHAR* argv[] );
static _TCHAR*  getProgramDir();
static _TCHAR* 	getDefaultOfficialName(_TCHAR* program);
static _TCHAR*  findStartupJar();
static _TCHAR*  findLibrary(_TCHAR* program);
static _TCHAR*  findFile( _TCHAR* path, _TCHAR* prefix);
 
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
	RunMethod runMethod;
	
	setlocale(LC_ALL, "");
	
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
    
	/* Find the startup.jar */
	jarFile = findStartupJar();
	if(jarFile == NULL) {
		errorMsg = malloc( (_tcslen(startupMsg) + _tcslen(officialName) + 10) * sizeof(_TCHAR) );
        _stprintf( errorMsg, startupMsg, officialName );
        displayMessage( officialName, errorMsg );
        free( errorMsg );
    	exit( 1 );
	}

	/* Find the eclipse library */
	library = findLibrary(program); 
	if(library == NULL) {
		errorMsg = malloc( (_tcslen(libraryMsg) + _tcslen(officialName) + 10) * sizeof(_TCHAR) );
        _stprintf( errorMsg, libraryMsg, officialName );
        displayMessage( officialName, errorMsg );
        free( errorMsg );
    	exit( 1 );
	}
	
	void * handle = loadLibrary(library);
	runMethod = findSymbol(library, RUN_METHOD);
	exitCode = runMethod(argc, argv);
	unloadLibrary(handle);
	
	free( jarFile );
    free( programDir );
    free( officialName );
    
	return exitCode;
}

/*
 * Parse arguments of the command.
 */
static void parseArgs( int* pArgc, _TCHAR* argv[] )
{
	Option* option;
    int     remArgs;
    int     index;
    int     i;

    /* Ensure the list of user argument is NULL terminated. */
    argv[ *pArgc ] = NULL;

	/* For each user defined argument (excluding the program) */
    for (index = 1; index < *pArgc; index++){
        remArgs = 0;

        /* Find the corresponding argument is a option supported by the launcher */
        option = NULL;
        for (i = 0; option == NULL && i < optionsSize; i++)
        {
        	if (_tcsicmp( argv[ index ], options[ i ].name ) == 0)
        	    option = &options[ i ];
       	}

       	/* If the option is recognized by the launcher */
       	if (option != NULL)
       	{
       		/* If the option requires a value and there is one, extract the value. */
       		if (option->value != NULL && (index+1) < *pArgc)
       			*option->value = argv[ index+1 ];

       		/* If the option requires a flag to be set, set it. */
       		if (option->flag != NULL)
       			*option->flag = 1;
       		remArgs = option->remove;
       	}

        /* All of the remaining arguments are user VM args. */
        else if (_tcsicmp( argv[ index ], VMARGS ) == 0)
        {
            userVMarg = &argv[ index+1 ];
            argv[ index ] = NULL;
            *pArgc = index;
        }

		/* Remove any matched arguments from the list. */
        if (remArgs > 0)
        {
            for (i = (index + remArgs); i <= *pArgc; i++)
            {
                argv[ i - remArgs ] = argv[ i ];
            }
            index--;
            *pArgc -= remArgs;
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

static _TCHAR* findStartupJar(){
	_TCHAR * file;
	_TCHAR * pluginsPath;
	struct _stat stats;
	
	if( startupArg != NULL ) {
		/* startup jar was specified on the command line */
		
		/* Construct the absolute name of the startup jar */
		file = malloc( (_tcslen( programDir ) + _tcslen( startupArg ) + 1) * sizeof( _TCHAR ) );
		file = _tcscpy( file, programDir );
	  	file = _tcscat( file, startupArg );
	
		/* If the file does not exist, treat the argument as an absolute path */
		if (_tstat( file, &stats ) != 0)
		{
			free( file );
			file = malloc( (_tcslen( startupArg ) + 1) * sizeof( _TCHAR ) );
			file = _tcscpy( file, startupArg );
			
			/* still doesn't exit? */
			if (_tstat( file, &stats ) != 0) {
				file = NULL;
			}
		}
		return file;
	}

	int pathLength = _tcslen(programDir);
	pluginsPath = malloc( (pathLength + 1 + 7) * sizeof(char));
	_tcscpy(pluginsPath, programDir);
	pluginsPath[pathLength] = dirSeparator;
	pluginsPath[pathLength + 1] = 0;
	_tcscat(pluginsPath, _T_ECLIPSE("plugins"));
	
	/* equinox startup jar? */	
	file = findFile(pluginsPath, _T_ECLIPSE("org.eclipse.equinox.startup"));
	if(file != NULL)
		return file;
		
	file = malloc( (_tcslen( DEFAULT_STARTUP ) + 1) * sizeof( _TCHAR ) );
	file = _tcscpy( file, DEFAULT_STARTUP );
	return file;
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

 /* 
 * Looks for files of the form /path/prefix_version.<extension> and returns the full path to
 * the file with the largest version number
 */ 
static _TCHAR* findFile( _TCHAR* path, _TCHAR* prefix)
{
	struct _stat stats;
	struct dirent *file;
	_tDIR *dir;
	int prefixLength;
	int pathLength;
	_TCHAR* candidate = NULL;
	_TCHAR* result = NULL;
	_TCHAR* fileName = NULL;
	
	/* does path exist? */
	if( _tstat(path, &stats) != 0 )
		return NULL;
	
	dir = _topendir(path);
	if(dir == NULL)
		return NULL;  /* can't open dir */
		
	prefixLength = _tcslen(prefix);
	while((file = _treaddir(dir)) != NULL) {
		fileName = file->d_name;
		if(_tcsncmp(fileName, prefix, prefixLength) == 0 && fileName[prefixLength] == _T_ECLIPSE('_')) {
			if(candidate == NULL)
				candidate = _tcsdup(fileName);
			else {
				/* compare, take the highest version */
				if( _tcscmp(candidate, fileName) < 0) {
					free(candidate);
					candidate = _tcsdup(fileName);
				}
			}
		}
	}
	_tclosedir(dir);

	if(candidate != NULL) {
		pathLength = _tcslen(path);
		result = malloc((pathLength + 1 + _tcslen(candidate)) * sizeof(_TCHAR));
		_tcscpy(result, path);
		result[pathLength] = dirSeparator;
		result[pathLength + 1] = 0;
		_tcscat(result, candidate);
		free(candidate);
		return result;
	}
	return NULL;
}
