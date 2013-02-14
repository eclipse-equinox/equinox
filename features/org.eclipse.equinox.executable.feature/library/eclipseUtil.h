/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *	   Martin Oberhuber (Wind River) - [149994] Add --launcher.appendVmargs
 *******************************************************************************/

#ifndef ECLIPSE_UTIL_H
#define ECLIPSE_UTIL_H

/* constants for checkProvidedVMType */
#define VM_NOTHING		0		/* NULL was given as input */
#define VM_OTHER		1		/* don't know, could be executable or could be nothing */
#define VM_DIRECTORY	2		/* it is a directory */
#define VM_LIBRARY		3		/* it is a library (isVmLibrary would return true) */
#define VM_EE_PROPS		4		/* it is a vm .ee properties file */

/* Eclipse Launcher Utility Methods */

/* Is the given Java VM J9 */
extern int isJ9VM( _TCHAR* vm );

/* Is the given file a shared library? */
extern int isVMLibrary( _TCHAR* vm );

/* determine what the provided -vm argument is referring to */ 
extern int checkProvidedVMType( _TCHAR* vm );

/* take a list of path separated with pathSeparator and run them through checkPath */
extern _TCHAR * checkPathList( _TCHAR* pathList, _TCHAR* programDir, int reverseOrder);

/* take a NULL terminated array of strings and concatenate them together into one string */
extern _TCHAR * concatStrings(_TCHAR** strs);

/* Concatenates two NULL-terminated arrays of strings into a new array of strings */
extern _TCHAR** concatArgs(_TCHAR** l1, _TCHAR** l2);

/* Returns the relative position of arg in the NULL-terminated list of args, or -1 */
extern int indexOf(_TCHAR *arg, _TCHAR **args);

/* take a NULL terminated array of strings and concatenate them together using the give pathSeparator */
extern _TCHAR* concatPaths(_TCHAR** paths, _TCHAR pathSeparator);

/* check that the buffer contains all the given paths */
extern int containsPaths(_TCHAR * str, _TCHAR** paths);

#ifdef AIX
/* Get the version of the VM */
extern char* getVMVersion( char* vm );
#endif

/* Compare JVM Versions */
extern int versionCmp( char* ver1, char* ver2 );

#endif /* ECLIPSE_UTIL_H */
