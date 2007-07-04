/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *******************************************************************************/

#include "eclipseOS.h"
#include "eclipseUtil.h"
#include "eclipseCommon.h"
#include "eclipseJNI.h"

#include <windows.h>
#include <commctrl.h>
#include <process.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/stat.h>

#ifdef __MINGW32__
#include <stdlib.h>
#endif

extern HWND topWindow;

/* Global Variables */
_TCHAR*  defaultVM     = _T("javaw.exe");
_TCHAR*  vmLibrary 	   = _T("jvm.dll");
_TCHAR*  shippedVMDir  = _T("jre\\bin\\");

/* Define the window system arguments for the Java VM. */
static _TCHAR*  argVM[] = { NULL };

/* Define local variables for running the JVM and detecting its exit. */
static int     jvmProcess     = 0;
static int     jvmExitCode    = 0;
static int     jvmExitTimeout = 100;
static int     jvmExitTimerId = 99;

static void CALLBACK  detectJvmExit( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime );
static _TCHAR* checkVMRegistryKey(HKEY jrekey, _TCHAR* subKeyName);
static void adjustSearchPath( _TCHAR * vmLibrary );
static _TCHAR* findLib( _TCHAR* command );

/* define default locations in which to find the jvm shared library
 * these are paths relative to the java exe, the shared library is
 * for example jvmLocations[0] + dirSeparator + vmLibrary */
#define MAX_LOCATION_LENGTH 25 /* none of the jvmLocations strings should be longer than this */ 
static const _TCHAR* jvmLocations [] = { _T("j9vm"), _T("..\\jre\\bin\\j9vm"),
										 _T("client"), _T("..\\jre\\bin\\client"), 
										 _T("server"), _T("..\\jre\\bin\\server"),
										 _T("classic"), _T("..\\jre\\bin\\classic"),
										 _T("jrockit"), _T("..\\jre\\bin\\jrockit"),
								 		 NULL };

/* for detecting sun vms */
typedef struct {
	WORD language;
	WORD codepage;
} TRANSLATIONS;

#define COMPANY_NAME_KEY _T_ECLIPSE("\\StringFileInfo\\%04x%04x\\CompanyName")
#define SUN_MICROSYSTEMS _T_ECLIPSE("Sun Microsystems")

/* Show the Splash Window
 *
 * Open the bitmap, insert into the splash window and display it.
 *
 */
int showSplash( const _TCHAR* featureImage )
{
	static int splashing = 0;
    HBITMAP hBitmap = 0;
    BITMAP  bmp;
    HDC     hDC;
    int     depth;
    int     x, y;
    int     width, height;

	if(splashing) {
		/*splash screen is already showing, do nothing */
		return 0;
	}
	if (featureImage == NULL)
		return -1;
	
	/* if Java was started first and is calling back to show the splash, we might not
	 * have initialized the window system yet
	 */
	initWindowSystem(0, NULL, 1);
	
    /* Load the bitmap for the feature. */
    hDC = GetDC( NULL);
    depth = GetDeviceCaps( hDC, BITSPIXEL ) * GetDeviceCaps( hDC, PLANES);
    ReleaseDC(NULL, hDC);
    if (featureImage != NULL)
    	hBitmap = LoadImage(NULL, featureImage, IMAGE_BITMAP, 0, 0, LR_LOADFROMFILE);

    /* If the bitmap could not be found, return an error. */
    if (hBitmap == 0)
    	return ERROR_FILE_NOT_FOUND;
    
	GetObject(hBitmap, sizeof(BITMAP), &bmp);

    /* figure out position */
    width = GetSystemMetrics (SM_CXSCREEN);
    height = GetSystemMetrics (SM_CYSCREEN);
    x = (width - bmp.bmWidth) / 2;
    y = (height - bmp.bmHeight) / 2;

	/* Centre the splash window and display it. */
    SetWindowPos (topWindow, 0, x, y, bmp.bmWidth, bmp.bmHeight, SWP_NOZORDER | SWP_NOSIZE | SWP_NOACTIVATE);
    SendMessage( topWindow, STM_SETIMAGE, IMAGE_BITMAP, (LPARAM) hBitmap );
    ShowWindow( topWindow, SW_SHOW );
    BringWindowToTop( topWindow );
	splashing = 1;
	
    /* Process messages */
	dispatchMessages();
	return 0;
}

void dispatchMessages() {
	MSG     msg;
	
	if(topWindow == 0)
		return;
	while (PeekMessage( &msg, NULL, 0, 0, PM_REMOVE))
   	{
		TranslateMessage( &msg );
		DispatchMessage( &msg );
	}
}

long getSplashHandle() {
	return (long)topWindow;
}

void takeDownSplash() {
	if(topWindow != NULL) {
		DestroyWindow(topWindow);
		dispatchMessages();
		topWindow = 0;
	}
}

/* Get the window system specific VM args */
_TCHAR** getArgVM( _TCHAR *vm )
{
	return argVM;
}

/* Local functions */

_TCHAR * findVMLibrary( _TCHAR* command ) {
	_TCHAR* lib = findLib(command);
	if( lib != NULL ) {
		adjustSearchPath(lib);
	}
	return lib;
}

void adjustSearchPath( _TCHAR* vmLib ){
	_TCHAR ** paths;
	_TCHAR * path = NULL, *newPath = NULL;
	_TCHAR * buffer, *c;
	int i, length;
	int needAdjust = 0, freePath = 0;
	
	/* we want the directory containing the library, and the parent directory of that */
	paths = (_TCHAR**) malloc( 2 * sizeof(_TCHAR*));
	buffer = _tcsdup(vmLib);	 
	for (i = 0; i < 2; i++ ){
		c = _tcsrchr(buffer, dirSeparator);
		*c = 0;
		length = _tcslen(buffer);
		paths[i] = malloc((length + 2) * sizeof(_TCHAR));
		_stprintf( paths[i], _T_ECLIPSE("%s%c"), buffer, pathSeparator );
	}	
	free(buffer);
	
	/* first call to GetEnvironmentVariable tells us how big to make the buffer */
	length = GetEnvironmentVariable(_T_ECLIPSE("PATH"), path, 0);
	if (length > 0) {
		path = malloc(length * sizeof(_TCHAR));
		GetEnvironmentVariable(_T_ECLIPSE("PATH"), path, length);
		freePath = 1;
	} else {
		path = _T_ECLIPSE("");
		freePath = 0;
		needAdjust = 1;
	}
	
	if(length > 0) {
		buffer = malloc((_tcslen(path) + 2) * sizeof(_TCHAR));
		_stprintf(buffer, _T_ECLIPSE("%s%c"), path, pathSeparator);
		/* check if each of our paths is already on the search path */
		for (i = 0; i < 2; i++) {
			c = _tcsstr(buffer, paths[i]);
			if ( c == NULL || !(c == buffer || *(c - 1) == pathSeparator))
			{
				/* entry not found */
				needAdjust = 1;
				break;
			}
		}
		free(buffer);
	}

	newPath = malloc((length + 1 + _tcslen(paths[0]) + 1 + _tcslen(paths[1]) + 1) * sizeof(_TCHAR));
	_stprintf(newPath, _T_ECLIPSE("%s%s%s"), paths[0], paths[1], path);

	SetEnvironmentVariable( _T_ECLIPSE("PATH"), newPath);
	
	for (i = 0; i < 2; i++)
		free(paths[i]);
	free(paths);
	free(newPath);
	if (freePath)
		free(path);
}
/*
 * Find the VM shared library starting from the java executable 
 */
static _TCHAR* findLib( _TCHAR* command ) {
	int i, j;
	int pathLength;	
	struct _stat stats;
	_TCHAR * path;				/* path to resulting jvm shared library */
	_TCHAR * location;			/* points to begining of jvmLocations section of path */
	
	/* for looking in the registry */
	HKEY jreKey = NULL;
	DWORD length = MAX_PATH;
	_TCHAR keyName[MAX_PATH];
	_TCHAR * jreKeyName;		
	
	if (command != NULL) {
		location = _tcsrchr( command, dirSeparator ) + 1;
		
		/*check first to see if command already points to the library */
		if (isVMLibrary(command)) {
			return command;
		}
		
		pathLength = location - command;
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + _tcslen(vmLibrary) + 1) * sizeof(_TCHAR));
		_tcsncpy(path, command, pathLength);
		location = &path[pathLength];
		 
		/* 
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		i = -1;
		while(jvmLocations[++i] != NULL) {
			_stprintf(location, _T_ECLIPSE("%s%c%s"), jvmLocations[i], dirSeparator, vmLibrary);
			if (_tstat( path, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return path;
			}
		}
		
		/* if command is eclipse/jre, don't look in registry */
		 location = malloc( (_tcslen( getProgramDir() ) + _tcslen( shippedVMDir ) + 1) * sizeof(_TCHAR) );
        _stprintf( location, _T_ECLIPSE("%s%s"), getProgramDir(), shippedVMDir );
        if( _tcsncmp(command, location, _tcslen(location)) == 0) {
        	free(location);
        	return NULL;
        }
        free(location);
	}
	
	/* Not found yet, try the registry, we will use the first vm >= 1.4 */
	jreKeyName = _T("Software\\JavaSoft\\Java Runtime Environment");
	if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, jreKeyName, 0, KEY_READ, &jreKey) == ERROR_SUCCESS) {
		if(RegQueryValueEx(jreKey, _T_ECLIPSE("CurrentVersion"), NULL, NULL, (void*)&keyName, &length) == ERROR_SUCCESS) {
			path = checkVMRegistryKey(jreKey, keyName);
			if (path != NULL) {
				RegCloseKey(jreKey);
				return path;
			}
		}
		j = 0;
		length = MAX_PATH;
		while (RegEnumKeyEx(jreKey, j++, keyName, &length, 0, 0, 0, 0) == ERROR_SUCCESS) {  
			/*look for a 1.4 or 1.5 vm*/ 
			if( _tcsncmp(_T("1.4"), keyName, 3) <= 0 ) {
				path = checkVMRegistryKey(jreKey, keyName);
				if (path != NULL) {
					RegCloseKey(jreKey);
					return path;
				}
			}
		}
		RegCloseKey(jreKey);
	}
	return NULL;
}

/*
 * Read the subKeyName subKey of jreKey and look to see if it has a Value 
 * "RuntimeLib" which points to a jvm library we can use 
 * 
 * Does not close jreKey
 */
static _TCHAR* checkVMRegistryKey(HKEY jreKey, _TCHAR* subKeyName) {
	_TCHAR value[MAX_PATH];
	HKEY subKey = NULL;
	DWORD length = MAX_PATH;
	_TCHAR *result = NULL;
	struct _stat stats;
	
	if(RegOpenKeyEx(jreKey, subKeyName, 0, KEY_READ, &subKey) == ERROR_SUCCESS) {				
		/*The RuntimeLib value should point to the library we want*/
		if(RegQueryValueEx(subKey, _T("RuntimeLib"), NULL, NULL, (void*)&value, &length) == ERROR_SUCCESS) {
			if (_tstat( value, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/*library exists*/
				result = _tcsdup(value);
			}
		}
		RegCloseKey(subKey);
	}
	return result;
}

static _TCHAR* buildCommandLine( _TCHAR* program, _TCHAR* args[] )
{
	int   index, length = 0;
	_TCHAR *commandLine, *ch, *space;

	/*
	* Build the command line. Any argument with spaces must be in
	* double quotes in the command line. 
	*/
	if(program != NULL) 
		length = _tcslen(program) + 1;
	for (index = 0; args[index] != NULL; index++)
	{
		/* String length plus space character */
		length += _tcslen( args[ index ] ) + 1;
		/* Quotes */
		if (_tcschr( args[ index ], _T(' ') ) != NULL) length += 2;
	}
	
	commandLine = ch = malloc ( (length + 1) * sizeof(_TCHAR) );
	if (program != NULL) {
		_tcscpy(ch, program);
		ch += _tcslen(program);
		*ch++ = _T(' ');
	}
	for (index = 0; args[index] != NULL; index++)
	{
		space = _tcschr( args[ index ], _T(' '));
		if (space != NULL) *ch++ = _T('\"');
		_tcscpy( ch, args[index] );
		ch += _tcslen( args[index] );
		if (space != NULL) *ch++ = _T('\"');
		*ch++ = _T(' ');
	}
	*ch = _T('\0');
	return commandLine;
}
void restartLauncher( _TCHAR* program, _TCHAR* args[] )
{
	_TCHAR* commandLine = buildCommandLine(program, args);
	
	{
	STARTUPINFO    si;
    PROCESS_INFORMATION  pi;
    GetStartupInfo(&si);
    if (CreateProcess(NULL, commandLine, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi)) {
    	CloseHandle( pi.hThread );
    }   
	}
	free(commandLine);
}

int launchJavaVM( _TCHAR* args[] )
{
	MSG msg;
	_TCHAR* commandLine;
	jvmProcess = -1;
	commandLine = buildCommandLine(NULL, args);
	
	/*
	* Start the Java virtual machine. Use CreateProcess() instead of spawnv()
	* otherwise the arguments cannot be freed since spawnv() segments fault.
	*/
	{
	STARTUPINFO    si;
    PROCESS_INFORMATION  pi;
    GetStartupInfo(&si);
    if (CreateProcess(NULL, commandLine, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi)) {
    	CloseHandle( pi.hThread );
    	jvmProcess = (int)pi.hProcess;
    }    
	}

	free( commandLine );

	/* If the child process (JVM) would not start */
	if (jvmProcess == -1)
	{
		/* Return the error number. */
		jvmExitCode = errno;
		jvmProcess  = 0;
	}

	/* else */
	else
	{
        /* Set a timer to detect JVM process termination. */
        SetTimer( topWindow, jvmExitTimerId, jvmExitTimeout, detectJvmExit );

    	/* Process messages until the JVM terminates.
    	   This launcher process must continue to process events until the JVM exits
    	   or else Windows 2K will hang if the desktop properties (e.g., background) are
    	   changed by the user. Windows does a SendMessage() to every top level window
    	   process, which blocks the caller until the process responds. */
   		while (jvmProcess != 0)
   		{
   			GetMessage( &msg, NULL, 0, 0 );
			TranslateMessage( &msg );
			DispatchMessage( &msg );
		}

		/* Kill the timer. */
        KillTimer( topWindow, jvmExitTimerId );
	}

	/* Return the exit code from the JVM. */
	return jvmExitCode;
}

/* Detect JVM Process Termination */
static void CALLBACK detectJvmExit( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime )
{
    DWORD   exitCode;

    /* If the JVM process has terminated */
    if (!GetExitCodeProcess( (HANDLE)jvmProcess, &exitCode ) ||
    		 exitCode != STILL_ACTIVE)
    {
    	/* Save the JVM exit code. This should cause the loop in startJavaVM() to exit. */
        jvmExitCode = exitCode;
        jvmProcess = 0;
    }
}

void processVMArgs(_TCHAR **vmargs[] ) {
	/* nothing yet */
}

int startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[] )
{
	return startJavaJNI(libPath, vmArgs, progArgs);
}

int isSunVM( _TCHAR * vm ) {
	int result = 0;
	DWORD infoSize;
	DWORD handle;
	void * info;
	
	_TCHAR * key, *value;
	int i, valueSize;
	
	if (vm == NULL)
		return 0;
	
	infoSize = GetFileVersionInfoSize(vm, &handle);
	if (infoSize > 0) {
		info = malloc(infoSize);
		if (GetFileVersionInfo(vm, 0,  infoSize, info)) {
			TRANSLATIONS * translations;
			int translationsSize;
			VerQueryValue(info,  _T_ECLIPSE("\\VarFileInfo\\Translation"), (void *) &translations, &translationsSize);
			
			/* this size is only right because %04x is 4 characters */
			key = malloc( (_tcslen(COMPANY_NAME_KEY) + 1) * sizeof(_TCHAR));
			for (i = 0; i < (translationsSize / sizeof(TRANSLATIONS)); i++) {
				_stprintf(key, COMPANY_NAME_KEY, translations[i].language, translations[i].codepage);
				
				VerQueryValue(info, key, (void *)&value, &valueSize);
				if (_tcsncmp(value, SUN_MICROSYSTEMS, _tcslen(SUN_MICROSYSTEMS)) == 0) {
					result = 1;
					break;
				}
			}
			free(key);
		}
		free(info);
	}
	return result;
}
