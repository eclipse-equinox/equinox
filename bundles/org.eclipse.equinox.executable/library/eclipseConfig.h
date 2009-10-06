/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#ifndef ECLIPSE_CONFIG_H
#define ECLIPSE_CONFIG_H

/* Configuration file reading utilities */

/**
 * Reads a configuration file for the corresponding
 * program argument.
 * e.g if the program argument contains "c:/folder/eclipse.exe"
 * then the config file "c:/folder/eclipse.ini" will be parsed.
 * On a Unix like platform, for a program argument "/usr/eclipse/eclipse"
 * should correspond a configuration file "/usr/eclipse/eclipse.ini"
 *
 * This method will call readConfigFile to read the actual ini file
 *
 * Returns 0 if success.
 */
extern int readIniFile(_TCHAR* program, int *argc, _TCHAR ***argv);

/**
 * Reads the given configuration file 
 * The argument argv refers to a newly allocated array of strings.
 * The first entry is the program name to mimic the expectations
 * from a typical argv list.
 * The last entry of that array is NULL. 
 * Each non NULL entry in that array must be freed by the caller 
 * as well as the array itself, using freeConfig().
 * The argument argc contains the number of string allocated.
 *
 * Returns 0 if success.
 */
extern int readConfigFile( _TCHAR * config_file, int *argc, _TCHAR ***argv );
/**
 * Free the memory allocated by readConfigFile().
 */
extern void freeConfig(_TCHAR **args);

#endif /* ECLIPSE_CONFIG_H */
