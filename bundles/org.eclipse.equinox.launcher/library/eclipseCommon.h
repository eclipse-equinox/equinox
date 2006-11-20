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
 
#ifndef ECLIPSE_COMMON_H
#define ECLIPSE_COMMON_H

#include "eclipseUnicode.h"

/* Variables and Methods that will be needed by both the executable and the library */

#define MAX_PATH_LENGTH   2000
#define STR(s) # s

#ifdef UNICODE
#define pathSeparator pathSeparatorW
#define dirSeparator dirSeparatorW
#define officialName officialNameW
#define parseArgs parseArgsW
#define displayMessage displayMessageW
#define getProgramDir getProgramDirW
#define findCommand findCommandW
#define findFile findFileW
#define loadLibrary loadLibraryW
#define unloadLibrary unloadLibraryW
#define findSymbol findSymbolW
#define run runW
#define setInitialArgs setInitialArgsW
#endif

#define RUN_METHOD 		 _T_ECLIPSE(STR(run))
#define SET_INITIAL_ARGS _T_ECLIPSE(STR(setInitialArgs))

extern _TCHAR   dirSeparator;         /* '/' or '\\' */
extern _TCHAR   pathSeparator;        /* separator used in PATH variable */
extern _TCHAR*  officialName;		  /* Program official name           */

 /*
 * Find the absolute pathname to where a command resides.
 *
 * The string returned by the function must be freed.
 */
extern _TCHAR* findCommand( _TCHAR* command );

extern _TCHAR* findFile( _TCHAR* path, _TCHAR* prefix);

/** Display a Message
 *
 * This method is called to display an error message to the user before exiting.
 * The method should not return until the user has acknowledged
 * the message. This method may be called before the window
 * system has been initialized. The program should exit after calling this method.
 */
extern void displayMessage( _TCHAR* title, _TCHAR* message );

/* Load the specified shared library
 */
extern void * loadLibrary( _TCHAR * library );

/* Unload the shared library
 */
extern void unloadLibrary( void * handle );
 
/* Find the given symbol in the shared library
 */
extern void * findSymbol( void * handle, _TCHAR * symbol );

#endif
