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
 *******************************************************************************/

#ifndef ECLIPSE_OS_H
#define ECLIPSE_OS_H

#include "eclipseJNI.h"
#include "eclipseUnicode.h"

#ifdef MACOSX
#define JAVA_FRAMEWORK "/System/Library/Frameworks/JavaVM.framework"
#endif

/* Operating System Dependent Information */

/*** See eclipse.c for information on the launcher runtime architecture ***/

/* Global Variables */
extern _TCHAR*  defaultVM;   			/* name of VM to use normally      */
#ifdef _WIN32
extern _TCHAR*  consoleVM;				/* windows needs a different vm executable for the console */
#endif
extern _TCHAR*  shippedVMDir;			/* VM bin directory with separator */
extern _TCHAR*  exitData;		  		/* exit data set from Java */
extern _TCHAR*  vmLibrary;				/* name of the VM shared library */
extern int		initialArgc;			/* argc originally used to start launcher */
extern _TCHAR**	initialArgv;			/* argv originally used to start launcher */
extern _TCHAR*  eeLibPath;				/* library path specified in a .ee file */
extern int		secondThread;			/* whether or not to start the vm on a second thread */

/* OS Specific Functions */

/** Display a Message
 *
 * This method is called to display a message to the user.
 * The method should not return until the user has acknowledged
 * the message. This method will only be called after the window
 * system has been initialized.
 */
extern void displayMessage( _TCHAR* title, _TCHAR* message );


/** Initialize the Window System
 *
 * This method is called after the command line arguments have been
 * parsed. Its purpose is to initialize the corresponding window system.
 *
 * The showSplash flag indicates the splash window will be displayed by
 * this process (e.g., value will be zero for the main launcher).
 */
extern int initWindowSystem( int* argc, _TCHAR* argv[], int showSplash );


/** Show the Splash Window
 *
 * This method is called to display the actual splash window. It will only
 * be called by the splash window process and not the main launcher process.
 * The splash ID passed corresponds to the string returned from initWindowSystem().
 * If possible, this ID should be used to communicate some piece of data back
 * to the main launcher program for two reasons:
 * 1) to detect when the splash window process terminates
 * 2) to terminate the splash window process should the JVM terminate before it
 *    completes its initialization.
 *
 * Two parameters are passed: the install home directory and a specific bitmap image
 * file for a feature. The feature's image file is tried first and if it cannot be
 * displayed, the images from the install directory are used.
 *
 * Return (exit code):
 * 0        - success
 * non-zero - could not find a splash image to display
 */
extern int showSplash( const _TCHAR* featureImage );

/** Get List of Java VM Arguments
 *
 * A given Java VM might require a special set of arguments in order to
 * optimize its performance. This method returns a NULL terminated array
 * of strings, where each string is a separate VM argument.
 */
extern _TCHAR** getArgVM( _TCHAR *vm );

/* Find the vm shared library associated with the given java executable */
extern _TCHAR * findVMLibrary( _TCHAR * command );

extern void dispatchMessages();

extern jlong getSplashHandle();

extern void takeDownSplash();

extern void restartLauncher( _TCHAR* program, _TCHAR* args[] );

/* launch the vm in a separate process and wait for it to finish */
extern JavaResults* launchJavaVM( _TCHAR* args[] );

/* launch the vm in this process using JNI invocation */
extern JavaResults* startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile );

/* do any platform specific processing of the user vmargs */
extern void processVMArgs(_TCHAR **vmargs[] );

/* is this a Sun VM, returns 0 if we don't know */
extern int isSunVM( _TCHAR * javaVM, _TCHAR * jniLib );

/* an array of paths that will need to be on the search path to load the vm shared library */
extern _TCHAR ** getVMLibrarySearchPath(_TCHAR * vmLibrary);

extern int reuseWorkbench(_TCHAR** filePath, int timeout);

#endif /* ECLIPSE_OS_H */

