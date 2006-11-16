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
#include "eclipseUnicode.h"

#ifdef _WIN32
#include <direct.h>
#else
#include <unistd.h>
#include <strings.h>
#endif
#include <stdlib.h>
#include <sys/stat.h>

/* Global Variables */
_TCHAR* officialName = NULL;

 /*
 * Find the absolute pathname to where a command resides.
 *
 * The string returned by the function must be freed.
 */
#define EXTRA 20
_TCHAR* findCommand( _TCHAR* command )
{
    _TCHAR*  cmdPath;
    int    length;
    _TCHAR*  ch;
    _TCHAR*  dir;
    _TCHAR*  path;
    struct _stat stats;

    /* If the command was an abolute pathname, use it as is. */
    if (command[0] == dirSeparator ||
       (_tcslen( command ) > 2 && command[1] == _T_ECLIPSE(':')))
    {
        length = _tcslen( command );
        cmdPath = malloc( (length + EXTRA) * sizeof(_TCHAR) ); /* add extra space for a possible ".exe" extension */
        _tcscpy( cmdPath, command );
    }

    else
    {
        /* If the command string contains a path separator */
        if (_tcschr( command, dirSeparator ) != NULL)
        {
            /* It must be relative to the current directory. */
            length = MAX_PATH_LENGTH + EXTRA + _tcslen( command );
            cmdPath = malloc( length * sizeof (_TCHAR));
            _tgetcwd( cmdPath, length );
            if (cmdPath[ _tcslen( cmdPath ) - 1 ] != dirSeparator)
            {
                length = _tcslen( cmdPath );
                cmdPath[ length ] = dirSeparator;
                cmdPath[ length+1 ] = _T_ECLIPSE('\0');
            }
            _tcscat( cmdPath, command );
        }

        /* else the command must be in the PATH somewhere */
        else
        {
            /* Get the directory PATH where executables reside. */
            path = _tgetenv( _T_ECLIPSE("PATH") );
            if (!path)
            {
	            return NULL;
            }
            else
            {
	            length = _tcslen( path ) + _tcslen( command ) + MAX_PATH_LENGTH;
	            cmdPath = malloc( length * sizeof(_TCHAR));
	
	            /* Foreach directory in the PATH */
	            dir = path;
	            while (dir != NULL && *dir != _T_ECLIPSE('\0'))
	            {
	                ch = _tcschr( dir, pathSeparator );
	                if (ch == NULL)
	                {
	                    _tcscpy( cmdPath, dir );
	                }
	                else
	                {
	                    length = ch - dir;
	                    _tcsncpy( cmdPath, dir, length );
	                    cmdPath[ length ] = _T_ECLIPSE('\0');
	                    ch++;
	                }
	                dir = ch; /* advance for the next iteration */

#ifdef _WIN32
                    /* Remove quotes */
	                if (_tcschr( cmdPath, _T_ECLIPSE('"') ) != NULL)
	                {
	                    int i = 0, j = 0, c;
	                    length = _tcslen( cmdPath );
	                    while (i < length) {
	                        c = cmdPath[ i++ ];
	                        if (c == _T_ECLIPSE('"')) continue;
	                        cmdPath[ j++ ] = c;
	                    }
	                    cmdPath[ j ] = _T_ECLIPSE('\0');
	                }
#endif
	                /* Determine if the executable resides in this directory. */
	                if (cmdPath[0] == _T_ECLIPSE('.') &&
	                   (_tcslen(cmdPath) == 1 || (_tcslen(cmdPath) == 2 && cmdPath[1] == dirSeparator)))
	                {
	                	_tgetcwd( cmdPath, MAX_PATH_LENGTH );
	                }
	                if (cmdPath[ _tcslen( cmdPath ) - 1 ] != dirSeparator)
	                {
	                    length = _tcslen( cmdPath );
	                    cmdPath[ length ] = dirSeparator;
	                    cmdPath[ length+1 ] = _T_ECLIPSE('\0');
	                }
	                _tcscat( cmdPath, command );
	
	                /* If the file is not a directory and can be executed */
	                if (_tstat( cmdPath, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
	                {
	                    /* Stop searching */
	                    dir = NULL;
	                }
	            }
	        }
        }
    }

#ifdef _WIN32
	/* If the command does not exist */
    if (_tstat( cmdPath, &stats ) != 0 || (stats.st_mode & S_IFREG) == 0)
    {
    	/* If the command does not end with .exe, append it an try again. */
    	length = _tcslen( cmdPath );
    	if (length > 4 && _tcsicmp( &cmdPath[ length - 4 ], _T_ECLIPSE(".exe") ) != 0)
    	    _tcscat( cmdPath, _T_ECLIPSE(".exe") );
    }
#endif

    /* Verify the resulting command actually exists. */
    if (_tstat( cmdPath, &stats ) != 0 || (stats.st_mode & S_IFREG) == 0)
    {
        free( cmdPath );
        cmdPath = NULL;
    }

    /* Return the absolute command pathname. */
    return cmdPath;
}
