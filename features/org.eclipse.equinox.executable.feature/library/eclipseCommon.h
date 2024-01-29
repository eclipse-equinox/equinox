/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at 
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Niefer
 *******************************************************************************/
 
#ifndef ECLIPSE_COMMON_H
#define ECLIPSE_COMMON_H

#include "eclipseUnicode.h"

/* Define constants for the options recognized by the launcher. */
#define CONSOLE            _T_ECLIPSE("-console")
#define CONSOLELOG         _T_ECLIPSE("-consoleLog")
#define DEBUG              _T_ECLIPSE("-debug")
#define OS                 _T_ECLIPSE("-os")
#define OSARCH             _T_ECLIPSE("-arch")
#define NOSPLASH           _T_ECLIPSE("-nosplash")
#define LAUNCHER           _T_ECLIPSE("-launcher")
#define SHOWSPLASH         _T_ECLIPSE("-showsplash")
#define EXITDATA           _T_ECLIPSE("-exitdata")
#define STARTUP            _T_ECLIPSE("-startup")
#define VM                 _T_ECLIPSE("-vm")
#define WS                 _T_ECLIPSE("-ws")
#define NAME               _T_ECLIPSE("-name")
#define VMARGS             _T_ECLIPSE("-vmargs")  /* special option processing required */
#define CP                 _T_ECLIPSE("-cp")
#define CLASSPATH          _T_ECLIPSE("-classpath")
#define JAR                _T_ECLIPSE("-jar")
#define PROTECT            _T_ECLIPSE("-protect")
#define ROOT               _T_ECLIPSE("root")  /* the only level of protection we care now */

#define OPENFILE           _T_ECLIPSE("--launcher.openFile")
#define DEFAULTACTION      _T_ECLIPSE("--launcher.defaultAction")
#define TIMEOUT            _T_ECLIPSE("--launcher.timeout")
#define LIBRARY            _T_ECLIPSE("--launcher.library")
#define SUPRESSERRORS      _T_ECLIPSE("--launcher.suppressErrors")
#define INI                _T_ECLIPSE("--launcher.ini")
#define APPEND_VMARGS      _T_ECLIPSE("--launcher.appendVmargs")
#define OVERRIDE_VMARGS    _T_ECLIPSE("--launcher.overrideVmargs")
#define SECOND_THREAD      _T_ECLIPSE("--launcher.secondThread")
#define PERM_GEN           _T_ECLIPSE("--launcher.XXMaxPermSize")
#define OLD_ARGS_START     _T_ECLIPSE("--launcher.oldUserArgsStart")
#define OLD_ARGS_END       _T_ECLIPSE("--launcher.oldUserArgsEnd")
#define SKIP_OLD_ARGS      _T_ECLIPSE("--launcher.skipOldUserArgs")

#define XXPERMGEN          _T_ECLIPSE("-XX:MaxPermSize=")
#define ADDMODULES         _T_ECLIPSE("--add-modules")
#define ACTION_OPENFILE    _T_ECLIPSE("openFile")
#define GTK_VERSION        _T_ECLIPSE("--launcher.GTK_version")

/* constants for ee options file */
#define EE_EXECUTABLE      _T_ECLIPSE("-Dee.executable=")
#define EE_CONSOLE         _T_ECLIPSE("-Dee.executable.console=")
#define EE_VM_LIBRARY      _T_ECLIPSE("-Dee.vm.library=")
#define EE_LIBRARY_PATH    _T_ECLIPSE("-Dee.library.path=")
#define EE_HOME            _T_ECLIPSE("-Dee.home=")
#define EE_FILENAME        _T_ECLIPSE("-Dee.filename=")
#define EE_HOME_VAR        _T_ECLIPSE("${ee.home}")

/* Variables and Methods that will be needed by both the executable and the library */

#define MAX_PATH_LENGTH   2000

#ifdef UNICODE
#define run runW
#define setInitialArgs setInitialArgsW
#define RUN_METHOD		 _T_ECLIPSE("runW")
#define SET_INITIAL_ARGS _T_ECLIPSE("setInitialArgsW")
#else
#define RUN_METHOD 		 _T_ECLIPSE("run")
#define SET_INITIAL_ARGS _T_ECLIPSE("setInitialArgs")
#endif

#define DEFAULT_EQUINOX_STARTUP _T_ECLIPSE("org.eclipse.equinox.launcher")

#ifdef _WIN32
#define IS_ABSOLUTE(path) (path[0] == _T_ECLIPSE('/') || path[0] == _T_ECLIPSE('\\') || (path[0] != 0 && path[1] == _T_ECLIPSE(':')))
#define IS_DIR_SEPARATOR(c) (c == _T_ECLIPSE('/') || c == _T_ECLIPSE('\\'))
#else
#define IS_ABSOLUTE(path) (path[0] == dirSeparator)
#define IS_DIR_SEPARATOR(c) (c == dirSeparator)
#endif

extern _TCHAR*  osArg;
extern _TCHAR*  osArchArg;
extern _TCHAR*  wsArg;

extern _TCHAR   dirSeparator;         /* '/' or '\\' */
extern _TCHAR   pathSeparator;        /* separator used in PATH variable */
extern _TCHAR* eclipseLibrary;		/* path the the eclipse_<ver>.so shared library */

extern char *toNarrow(const _TCHAR* src);

 /*
 * Find the absolute pathname to where a command resides.
 *
 * The string returned by the function must be freed.
 * Symlinks are resolved
 */
extern _TCHAR* findCommand( _TCHAR* command );

/*
 * Same as findCommand but optionally resolve symlinks
 */
extern _TCHAR* findSymlinkCommand( _TCHAR* command, int resolve );

extern _TCHAR* findFile( _TCHAR* path, _TCHAR* prefix);

extern _TCHAR* getProgramDir();

extern _TCHAR* getOfficialName();

extern void setOfficialName(_TCHAR * name);

extern _TCHAR* getProgramPath();

extern void setProgramPath(_TCHAR* name);

extern _TCHAR* resolveSymlinks( _TCHAR* path );

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

/* check the given path and attempt to make it absolute if it is relative */
extern _TCHAR* checkPath( _TCHAR* path, _TCHAR* programDir, int reverseOrder );

extern _TCHAR * lastDirSeparator(_TCHAR* str);

extern _TCHAR * firstDirSeparator(_TCHAR* str);
#endif
