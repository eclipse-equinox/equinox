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

#define ECLIPSE_ICON  401

/* Global Variables */
_TCHAR   dirSeparator  = _T('\\');
_TCHAR   pathSeparator = _T(';');
_TCHAR*  consoleVM     = _T("java.exe");
_TCHAR*  defaultVM     = _T("javaw.exe");
_TCHAR*  vmLibrary 	   = _T("jvm.dll");
_TCHAR*  shippedVMDir  = _T("jre\\bin\\");

/* Define the window system arguments for the Java VM. */
static _TCHAR*  argVM[] = { NULL };

/* Define local variables for the main window. */
static HWND    topWindow  = 0;
static WNDPROC oldProc;

/* define default locations in which to find the jvm shared library
 * these are paths relative to the java exe, the shared library is
 * for example jvmLocations[0] + dirSeparator + vmLibrary */
#define MAX_LOCATION_LENGTH 10 /* none of the jvmLocations strings should be longer than this */ 
static const _TCHAR* jvmLocations [] = { _T("j9vm"),
										 _T("client"), 
										 _T("server"), 
										 _T("classic"), 
								 		 NULL };

/* Define local variables for handling the splash window and its image. */
static int      splashTimerId = 88;

/* Local functions */
static void CALLBACK  splashTimeout( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime );
static LRESULT WINAPI WndProc (HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);

/* Display a Message */
void displayMessage( _TCHAR* title, _TCHAR* message )
{
	MessageBox( topWindow, message, title, MB_OK );
}

/* Initialize Window System
 *
 * Create a pop window to display the bitmap image.
 *
 * Return the window handle as the data for the splash command.
 *
 */
void initWindowSystem( int* pArgc, _TCHAR* argv[], int showSplash )
{
    /* Create a window that has no decorations. */
	InitCommonControls();
    topWindow = CreateWindowEx (0,
		_T("STATIC"),
		officialName,
		SS_BITMAP | WS_POPUP,
		0,
		0,
		0,
		0,
		NULL,
		NULL,
		GetModuleHandle (NULL),
		NULL);
	SetClassLong(topWindow, GCL_HICON, (LONG)LoadIcon(GetModuleHandle(NULL), MAKEINTRESOURCE(ECLIPSE_ICON)));
    oldProc = (WNDPROC) GetWindowLong (topWindow, GWL_WNDPROC);
    SetWindowLong (topWindow, GWL_WNDPROC, (LONG) WndProc);
}

/* Show the Splash Window
 *
 * Open the bitmap, insert into the splash window and display it.
 *
 */
int showSplash( _TCHAR* timeoutString, _TCHAR* featureImage )
{
	int     timeout = 0;
    RECT    rect;
    HBITMAP hBitmap = 0;
    HDC     hDC;
    int     depth;
    int     x, y;
    int     width, height;
    MSG     msg;

	/* Determine the splash timeout value (in seconds). */
	if (timeoutString != NULL && _tcslen( timeoutString ) > 0)
	{
	    _stscanf( timeoutString, _T("%d"), &timeout );
	}

    /* Load the bitmap for the feature. */
    hDC = GetDC( NULL);
    depth = GetDeviceCaps( hDC, BITSPIXEL ) * GetDeviceCaps( hDC, PLANES);
    ReleaseDC(NULL, hDC);
    if (featureImage != NULL)
    	hBitmap = LoadImage(NULL, featureImage, IMAGE_BITMAP, 0, 0, LR_LOADFROMFILE);

    /* If the bitmap could not be found, return an error. */
    if (hBitmap == 0)
    	return ERROR_FILE_NOT_FOUND;

	/* Load the bitmap into the splash popup window. */
    SendMessage( topWindow, STM_SETIMAGE, IMAGE_BITMAP, (LPARAM) hBitmap );

    /* Centre the splash window and display it. */
    GetWindowRect (topWindow, &rect);
    width = GetSystemMetrics (SM_CXSCREEN);
    height = GetSystemMetrics (SM_CYSCREEN);
    x = (width - (rect.right - rect.left)) / 2;
    y = (height - (rect.bottom - rect.top)) / 2;
    SetWindowPos (topWindow, 0, x, y, 0, 0, SWP_NOZORDER | SWP_NOSIZE | SWP_NOACTIVATE);
    ShowWindow( topWindow, SW_SHOW );
    BringWindowToTop( topWindow );

	/* If a timeout for the splash window was given */
	if (timeout != 0)
	{
		/* Add a timeout (in milliseconds) to bring down the splash screen. */
        SetTimer( topWindow, splashTimerId, (timeout * 1000), splashTimeout );
	}

    /* Process messages until the splash window is closed or process is terminated. */
   	while (GetMessage( &msg, NULL, 0, 0 ))
   	{
		TranslateMessage( &msg );
		DispatchMessage( &msg );
	}

	return 0;
}


/* Get the window system specific VM args */
_TCHAR** getArgVM( _TCHAR *vm )
{
	return argVM;
}

/* Local functions */

/* Splash Timeout */
static void CALLBACK splashTimeout( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime )
{
	/* Kill the timer. */
    KillTimer( topWindow, id );
	PostMessage( topWindow, WM_QUIT, 0, 0 );
}

/* Window Procedure for the Spash window.
 *
 * A special WndProc is needed to return the proper vlaue for WM_NCHITTEST.
 * It must also detect the message from the splash window process.
 */
static LRESULT WINAPI WndProc (HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
	switch (uMsg)
	{
		case WM_NCHITTEST: return HTCLIENT;
		case WM_CLOSE:
	    	PostQuitMessage(  0 );
	    	break;
	}
	return CallWindowProc (oldProc, hwnd, uMsg, wParam, lParam);
}

/* Load the specified shared library
 */
void * loadLibrary( _TCHAR * library ){
	return LoadLibrary(library);
}

/* Unload the shared library
 */
void unloadLibrary( void * handle ){
	FreeLibrary(handle);
}
 
/* Find the given symbol in the shared library
 */
void * findSymbol( void * handle, char * symbol ){
	return GetProcAddress(handle, symbol);
}

/*
 * Find the VM shared library starting from the java executable 
 */
_TCHAR* findVMLibrary( _TCHAR* command ) {
	int i;
	int pathLength;	
	struct _stat stats;
	_TCHAR * path;				/* path to resulting jvm shared library */
	_TCHAR * location;			/* points to begining of jvmLocations section of path */
	
	if (command != NULL) {
		location = _tcsrchr( command, dirSeparator ) + 1;
		
		/*check first to see if command already points to the library */
		if (_tcscmp(location, vmLibrary) == 0) {
			return command;
		}
		
		pathLength = location - command;
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + _tcslen(vmLibrary)) * sizeof(_TCHAR *));
		_tcsncpy(path, command, pathLength);
		location = &path[pathLength];
		 
		/* 
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		i = -1;
		while(jvmLocations[++i] != NULL) {
			int length = _tcslen(jvmLocations[i]);			
			_tcscpy(location, jvmLocations[i]);
			location[length] = dirSeparator;
			location[length + 1] = _T('\0');
			_tcscat(location, vmLibrary);
			if (_tstat( path, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return path;
			}
		}
	}
	
	/* Not found yet, try the registry, we will use the first 1.4 or 1.5 vm we can find*/
	HKEY keys[2] = { HKEY_CURRENT_USER, HKEY_LOCAL_MACHINE };
	_TCHAR * jreKeyName = _T("Software\\JavaSoft\\Java Runtime Environment");
	for (i = 0; i < 2; i++) {
		HKEY jreKey = NULL;
		if (RegOpenKeyEx(keys[i], jreKeyName, 0, KEY_READ, &jreKey) == ERROR_SUCCESS) {
			int j = 0;
			_TCHAR keyName[MAX_PATH];
			DWORD length = MAX_PATH;
			while (RegEnumKeyEx(jreKey, j++, keyName, &length, 0, 0, 0, 0) == ERROR_SUCCESS) {  
				/*look for a 1.4 or 1.5 vm*/ 
				if( _tcsncmp(_T("1.4"), keyName, 3) == 0 || _tcsncmp(_T("1.5"), keyName, 3) == 0) {
					HKEY subKey = NULL;
					if(RegOpenKeyEx(jreKey, keyName, 0, KEY_READ, &subKey) == ERROR_SUCCESS) {
						length = MAX_PATH;
						_TCHAR lib[MAX_PATH];
						/*The RuntimeLib value should point to the library we want*/
						if(RegQueryValueEx(subKey, _T("RuntimeLib"), NULL, NULL, (void*)&lib, &length) == ERROR_SUCCESS) {
							if (_tstat( lib, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
							{	/*library exists*/
								path = malloc( length * sizeof(TCHAR*));
								path[0] = _T('\0');
								_tcscat(path, lib);
								
								RegCloseKey(subKey);
								RegCloseKey(jreKey);
								return path;
							}
						}
						RegCloseKey(subKey);
					}
				}
			}
			RegCloseKey(jreKey);
		}
	}
	return NULL;
}
