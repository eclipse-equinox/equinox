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

#ifdef UNICODE
HINSTANCE g_hInstance = NULL;
BOOL WINAPI DllMain(HANDLE hInstDLL, DWORD dwReason, LPVOID lpvReserved)
{
	if (dwReason == DLL_PROCESS_ATTACH) {
		if (g_hInstance == NULL) g_hInstance = hInstDLL;
	}
	return TRUE;
}
#else
extern HINSTANCE g_hInstance;
#endif

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
	HINSTANCE module = GetModuleHandle(NULL);
	HICON icon = NULL;
	
	if(initialized)
		return;
    /* Create a window that has no decorations. */
    
	InitCommonControls();
    topWindow = CreateWindowEx ( WS_EX_TOOLWINDOW,
		_T("STATIC"),
		getOfficialName(),
		SS_BITMAP | WS_POPUP | WS_CLIPCHILDREN,
		CW_USEDEFAULT,
		0,
		CW_USEDEFAULT,
		0,
		NULL,
		NULL,
		module,
		NULL);

    icon = LoadIcon(module, MAKEINTRESOURCE(ECLIPSE_ICON));
    if (icon != NULL)
    	SetClassLong(topWindow, GCL_HICON, (LONG)icon);

	initialized = 1;
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
