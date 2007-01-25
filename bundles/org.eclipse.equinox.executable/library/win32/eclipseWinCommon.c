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
 
#include "eclipseCommon.h"
#include "eclipseOS.h"

#include <windows.h>
#include <stdlib.h>
#include <commctrl.h>

#define ECLIPSE_ICON  401

_TCHAR   dirSeparator  = _T('\\');
_TCHAR   pathSeparator = _T(';');

void initWindowSystem( int* pArgc, _TCHAR* argv[], int showSplash );
/*static LRESULT WINAPI WndProc (HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);*/

/* Global Main Window*/
#ifdef UNICODE
extern HWND topWindow;
#else
HWND    topWindow = 0;
#endif

/* Define local variables for the main window. */
/*static WNDPROC oldProc;*/

static int initialized = 0;

/* Display a Message */
void displayMessage( _TCHAR* title, _TCHAR* message )
{
	if(!initialized)
		initWindowSystem(0, NULL, 0);
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
	if(initialized)
		return;
    /* Create a window that has no decorations. */
    
	InitCommonControls();
    topWindow = CreateWindowEx (0,
		_T("STATIC"),
		getOfficialName(),
		SS_BITMAP | WS_POPUP | WS_CLIPCHILDREN,
		CW_USEDEFAULT,
		0,
		CW_USEDEFAULT,
		0,
		NULL,
		NULL,
		GetModuleHandle (NULL),
		NULL);
	SetClassLong(topWindow, GCL_HICON, (LONG)LoadIcon(GetModuleHandle(NULL), MAKEINTRESOURCE(ECLIPSE_ICON)));
/*    
    oldProc = (WNDPROC) GetWindowLong (topWindow, GWL_WNDPROC);
    SetWindowLong (topWindow, GWL_WNDPROC, (LONG) WndProc);
*/  
	initialized = 1;
}

/* Window Procedure for the Spash window.
 *
 * A special WndProc is needed to return the proper vlaue for WM_NCHITTEST.
 * It must also detect the message from the splash window process.
 */
/*static LRESULT WINAPI WndProc (HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
	switch (uMsg)
	{
		case WM_NCHITTEST: return HTCLIENT;
		case WM_CLOSE:
	    	PostQuitMessage(  0 );
	    	break;
	}
	return CallWindowProc (oldProc, hwnd, uMsg, wParam, lParam);
}*/

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
void * findSymbol( void * handle, _TCHAR * symbol ){
	char * str = NULL;
	void * result;
	
	str = toNarrow(symbol);
	result = GetProcAddress(handle, str);
	free(str);
	return result;
}

_TCHAR* resolveSymlinks( _TCHAR* path ) {
	/* no symlinks on windows */
	return path;
}
