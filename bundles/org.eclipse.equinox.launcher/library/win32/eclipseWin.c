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

#ifdef __MINGW32__
#include <stdlib.h>
#endif

#define ECLIPSE_ICON  401

/* Global Variables */
_TCHAR   dirSeparator  = _T('\\');
_TCHAR   pathSeparator = _T(';');
_TCHAR*  consoleVM     = _T("java.exe");
_TCHAR*  defaultVM     = _T("javaw.exe");
_TCHAR*  shippedVMDir  = _T("jre\\bin\\");

/* Define the window system arguments for the Java VM. */
static _TCHAR*  argVM[] = { NULL };

/* Define local variables for the main window. */
static HWND    topWindow  = 0;
static WNDPROC oldProc;

/* Define local variables for running the JVM and detecting its exit. */
static int     jvmProcess     = 0;
static int     jvmExitCode    = 0;
static int     jvmExitTimeout = 100;
static int     jvmExitTimerId = 99;

/* Define local variables for handling the splash window and its image. */
static int      splashTimerId = 88;

/* Local functions */
static void CALLBACK  detectJvmExit( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime );
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


/* Start the Java VM
 *
 * This method is called to start the Java virtual machine and to wait until it
 * terminates. The function returns the exit code from the JVM.
 */
int startJavaVM( _TCHAR* args[] )
{
    MSG   msg;
	int   index, length;
	_TCHAR *commandLine, *ch, *space;

	/*
	* Build the command line. Any argument with spaces must be in
	* double quotes in the command line. 
	*/
	length = 0;
	for (index = 0; args[index] != NULL; index++)
	{
		/* String length plus space character */
		length += _tcslen( args[ index ] ) + 1;
		/* Quotes */
		if (_tcschr( args[ index ], _T(' ') ) != NULL) length += 2;
	}
	commandLine = ch = malloc ( (length + 1) * sizeof(_TCHAR) );
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

/* Local functions */

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
