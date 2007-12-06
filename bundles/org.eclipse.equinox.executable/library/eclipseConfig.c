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

#include "eclipseOS.h"
#include "eclipseConfig.h"

#ifdef _WIN32

#include <stdio.h>
#include <sys/stat.h>

#ifdef __MINGW32__
#include <stdlib.h>
#endif

#else /* Unix like platforms */

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>

#endif

int readIniFile(_TCHAR* program, int *argc, _TCHAR ***argv) 
{
	_TCHAR* config_file = NULL;
	int result;
	
	if (program == NULL || argc == NULL || argv == NULL) return -1;

	/* Get a copy with room for .ini at the end */
	config_file = malloc( (_tcslen(program) + 5) * sizeof(_TCHAR));
	_tcscpy(config_file, program);
	
#ifdef _WIN32
	{
		/* Search for the extension .exe and replace it with .ini */
		_TCHAR *extension = _tcsrchr(config_file, _T_ECLIPSE('.'));
		if (extension == NULL || _tcslen(extension) < 4)
		{
			/* does not end with an extension, just append .ini */
			extension = config_file + _tcslen(config_file);
		}
		_tcscpy(extension, _T_ECLIPSE(".ini"));
#ifdef _WIN32_CONSOLE
		{
			/* We are the console version, if the ini file does not exist, try
			 * removing the 'c' from the end of the program name */
			struct _stat stats; 
			if (_tstat( config_file, &stats ) != 0 && *(extension - 1) == _T('c')) {
				_tcscpy(extension - 1, extension);
			}
		}
#endif
	}
#else
	/* Append the extension */
	strcat(config_file, ".ini");
#endif
	
	result = readConfigFile(config_file, argc, argv);
	free(config_file);
	return result;
}

int readConfigFile( _TCHAR * config_file, int *argc, _TCHAR ***argv )
{
	_TCHAR buffer[1024];
	_TCHAR argument[1024];
	_TCHAR * arg;
	FILE *file = NULL;
	int maxArgs = 128;
	int index;
	size_t length;
	
	/* Open the config file as a text file 
	 * Note that carriage return-linefeed combination \r\n are automatically
	 * translated into single linefeeds on input in the t (translated) mode
	 * on windows, on other platforms we will strip the \r as whitespace.
	 */	
	file = _tfopen(config_file, _T_ECLIPSE("rt"));	
	if (file == NULL) return -3;

	*argv = (_TCHAR **)malloc((1 + maxArgs) * sizeof(_TCHAR*));
	
	index = 0;
	
	/* Parse every line */	
	while (_fgetts(buffer, 1024, file) != NULL)
	{
		/* Extract the string prior to the first newline character.
		 * We don't have to worry about \r\n combinations since the file
		 * is opened in translated mode.
		 */
		if (_stscanf(buffer, _T_ECLIPSE("%[^\n]"), argument) == 1)
		{
			arg = _tcsdup(argument);
			length = _tcslen(arg);
			
			/* watch for comments */
			if(arg[0] == _T_ECLIPSE('#'))
				continue;
			
			/* basic whitespace trimming */
			while (length > 0 && (arg[length - 1] == _T_ECLIPSE(' ')  || 
					              arg[length - 1] == _T_ECLIPSE('\t') || 
					              arg[length - 1] == _T_ECLIPSE('\r'))) 
			{
				arg[--length] = 0;
			}
			/* ignore empty lines */
			if (length == 0)
				continue;
			
			(*argv)[index] = arg;
			index++;
			
			/* Grow the array of TCHAR*. Ensure one more entry is
			 * available for the final NULL entry
			 */
			if (index == maxArgs - 1)
			{
				maxArgs += 128;
				*argv = (_TCHAR **)realloc(*argv, maxArgs * sizeof(_TCHAR*));
			}
		}
	}
	(*argv)[index] = NULL;
	*argc = index;
	
	fclose(file);
	
	return 0;
}

void freeConfig(_TCHAR **argv) 
{
	int index = 0;
	if (argv == NULL) return;
	while (argv[index] != NULL)
	{
		free(argv[index]);
		index++;
	}
	free(argv);
}
