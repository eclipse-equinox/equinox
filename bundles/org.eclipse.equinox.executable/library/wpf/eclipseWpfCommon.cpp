/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Niefer
 *******************************************************************************/
 

extern "C" {

#include "eclipseCommon.h"
#include "eclipseOS.h"

#include <windows.h>
#include <stdlib.h>
#include <commctrl.h>


#define ECLIPSE_ICON  401

_TCHAR   dirSeparator  = _T('\\');
_TCHAR   pathSeparator = _T(';');

static int initialized = 0;

/* Load the specified shared library
 */
void * loadLibrary( _TCHAR * library ){
	return (void *)LoadLibrary(library);
}

/* Unload the shared library
 */
void unloadLibrary( void * handle ){
	FreeLibrary((HMODULE)handle);
}
 
/* Find the given symbol in the shared library
 */
void * findSymbol( void * handle, _TCHAR * symbol ){
	char * str = NULL;
	void * result;
	
	str = toNarrow(symbol);
	result = GetProcAddress((HMODULE)handle, str);
	free(str);
	return result;
}

_TCHAR* resolveSymlinks( _TCHAR* path ) {
	/* no symlinks on windows */
	return path;
}

} //end extern C

/* Display a Message */
void displayMessage( _TCHAR* title, _TCHAR* message )
{
	if(!initialized) 
		initWindowSystem(0, NULL, 0);
	
	System::String^ titleStr = gcnew System::String (title);
	System::String^ messageStr = gcnew System::String (message);		
	System::Windows::MessageBox::Show (messageStr, titleStr,  System::Windows::MessageBoxButton::OK);
}

/* Initialize Window System
 *
 * Create a pop window to display the bitmap image.
 *
 * Return the window handle as the data for the splash command.
 *
 */
int initWindowSystem( int* pArgc, _TCHAR* argv[], int showSplash )
{
	
	if(initialized)
		return 0;
	initialized = 1;
	return 0;
}
