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

/* Eclipse Mozilla Utility Methods */

#ifdef MOZILLA_FIX

#include "eclipseMozilla.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <sys/stat.h>


/* Filter function used by fixEnvForMozilla() for finding directories
 * with a desired prefix.
 */
int filter(const struct dirent *dir)
{
	char* prefixes[] = {
		"xulrunner-",
		"mozilla-",
		"firefox-",
		NULL
	};
	int XULRUNNER_INDEX = 0;
	char* root = "/usr/lib/";
	char* testlib = "/components/libwidget_gtk2.so";
	struct stat buf;
	int index = 0;
	char* dirname = (char *)dir->d_name;

	char* prefix = prefixes [index];
	while (prefix != NULL)
	{
		if (strncmp(dirname, prefix, strlen(prefix)) == 0)
		{
			/* If a xulrunner install is found then success is immediate since
			 * xulrunner always provides an embeddable GRE.
			 */
			if (index == XULRUNNER_INDEX) return 1;	/* include in scandir result */
			char* testpath = malloc (strlen(root) + strlen(dirname) + strlen(testlib) + 1);
			strcpy(testpath, root);
			strcat(testpath, dirname);
			strcat(testpath, testlib);
			int success = stat(testpath, &buf) == 0;
			free(testpath);
			if (success)
			{
				return 1;	/* include in scandir result */
			}
		}
		prefix = prefixes [++index];
	}
	return 0;	/* exclude from scandir result */
}

/* Set the environmnent required by the SWT Browser widget to bind to Mozilla. 
 * The SWT Browser widget relies on Mozilla on Linux. The LD_LIBRARY_PATH
 * and the Mozilla environment variable MOZILLA_FIVE_HOME must point
 * to the installation directory of Mozilla.
 * 
 * 1. Use the location set by MOZILLA_FIVE_HOME if it is defined
 * 2. Parse the file /etc/gre.conf if it is defined. This file is
 *    set by the RedtHat RPM manager.
 * 3. Try some common installation locations.
 */
void fixEnvForMozilla() {
	static int fixed = 0;
	if (fixed) return; 
	{
		char *ldPath = (char*)getenv("LD_LIBRARY_PATH");
		char *mozillaFiveHome = (char*)getenv("MOZILLA_FIVE_HOME");
		char *grePath = NULL; /* Gecko Runtime Environment Location */
		fixed = 1;
		/* Always dup the string so we can free later */
		if (ldPath != NULL) ldPath = strdup(ldPath);
		else ldPath = strdup("");
		
		/* MOZILLA_FIVE_HOME (if defined) points to the Mozilla
		 * install directory. Don't look any further if it is set.
		 */
		if (mozillaFiveHome != NULL) 
		{
			grePath = strdup(mozillaFiveHome);
		}

		/* The file gre.conf (if available) points to the
		 * Mozilla install directory. Don't look any further if 
		 * it is set.
		 */
		if (grePath == NULL)
		{
			struct stat buf;
			FILE *file = NULL;
#if defined(__amd64__) || defined(__x86_64__)
			if (stat("/etc/gre64.conf", &buf) == 0)
			{
				file = fopen("/etc/gre64.conf", "r");
			}
			else if (stat("/etc/gre.d/gre64.conf", &buf) == 0)
			{
				file = fopen("/etc/gre.d/gre64.conf", "r");
			} else
#endif
			if (stat("/etc/gre.conf", &buf) == 0)
			{
				file = fopen("/etc/gre.conf", "r");
			}
			else if (stat("/etc/gre.d/gre.conf", &buf) == 0)
			{
				file = fopen("/etc/gre.d/gre.conf", "r");
			}
			if (file != NULL)
			{
				char buffer[1024];
				char path[1024];
				while (fgets(buffer, 1024, file) != NULL)
				{
					if (sscanf(buffer, "GRE_PATH=%s", path) == 1)
					{
						grePath = strdup(path);
						break;
					}
				}
				fclose(file);
			}
		}

		/* Try some common installation locations. */
		if (grePath == NULL)
		{
			/* some other typical installation locations */
			char* dirs[] = {
				"/usr/lib/mozilla/",
				"/usr/local/mozilla/",
				"/opt/mozilla/",
				"/usr/lib/firefox/",
				"/usr/local/firefox/",
				"/opt/firefox/",
				"/usr/lib/MozillaFirebird/",
				"/usr/local/MozillaFirebird/",
				"/opt/MozillaFirebird/",
				NULL
			};
			char* testlib = "components/libwidget_gtk2.so";
			struct stat buf;
			int index = 0;
	
			char* dir = dirs [index++];
			while (dir != NULL)
			{
				char* testpath = malloc (strlen(dir) + strlen(testlib) + 1);
				strcpy(testpath, dir);
				strcat(testpath, testlib);
				int success = stat(testpath, &buf) == 0;
				free(testpath);
				if (success)
				{
					grePath = strdup(dir);
					break;
				}
				dir = dirs [index++];
			}

			if (grePath == NULL)
			{
				/* now try xulrunner-*, mozilla-*, firefox-* directories in /usr/lib/ */
				char* dir = "/usr/lib/";
				struct dirent **namelist;
				int i;

				int count = scandir(dir, &namelist, filter, alphasort);
				if (count > 0)
				{
					/* count-1 is used below in an attempt to get the matched directory
					 * with the latest version number.
					 */
					char* name = namelist [count - 1]->d_name;
					grePath = malloc (strlen(dir) + strlen(name) + 1);
					strcpy(grePath, dir);
					strcat(grePath, name);
					for (i = 0; i < count; i++) {
						free(namelist [i]);
					}
					free(namelist);
				}
			}
		}

		if (grePath != NULL)
		{
			ldPath = (char*)realloc(ldPath, strlen(ldPath) + strlen(grePath) + 2);
			if (strlen(ldPath) > 0) strcat(ldPath, ":");
			strcat(ldPath, grePath);
			setenv("LD_LIBRARY_PATH", ldPath, 1);
			
			if (mozillaFiveHome == NULL) setenv("MOZILLA_FIVE_HOME", grePath, 1);
			free(grePath);
		}
		free(ldPath);
	}
}
#endif /* MOZILLA_FIX */
