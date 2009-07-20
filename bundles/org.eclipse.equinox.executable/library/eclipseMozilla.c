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
		"mozilla-seamonkey-",
		"seamonkey-",
		"mozilla-",
		"mozilla-firefox-",
		"firefox-",
		NULL
	};
	int XULRUNNER_INDEX = 0;
#if defined(__amd64__) || defined(__x86_64__) || defined(__powerpc64__)
	char* root = "/usr/lib64/";
#else
	char* root = "/usr/lib/";
#endif

#if defined (SOLARIS)
	/*
	 * The solaris compiler does not do static linking, so just check
	 * for a common lib to ensure that the install seems valid.
	 */
	char* testlib = "/libxpcom.so";
#else
	/* Ensure that the install is dynamically-linked and is built with GTK2 */
	char* testlib = "/components/libwidget_gtk2.so";
#endif

	struct stat buf;
	int index = 0;
	char* dirname = (char *)dir->d_name;

	char* prefix = prefixes [index];
	while (prefix != NULL)
	{
		int prefixLength = strlen(prefix);
		if (strncmp(dirname, prefix, prefixLength) == 0)
		{
			/* If a xulrunner install is found then success is immediate since
			 * xulrunner always provides an embeddable GRE.
			 */
			if (index == XULRUNNER_INDEX) return 1;	/* include in scandir result */

			/* Check if the first character following the prefix is a numeric digit.
			 * This ensures that the suffix represents a version number like
			 * "mozilla-1.7.3", and not a different product like "mozilla-thunderbird".
			 */
			int dirLength = strlen(dirname);
			if (dirLength == prefixLength || ('0' <= dirname[prefixLength] && dirname[prefixLength] <= '9')) {
				char* testpath = malloc (strlen(root) + dirLength + strlen(testlib) + 1);
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
		}
		prefix = prefixes [++index];
	}
	return 0;	/* exclude from scandir result */
}

#if defined (SOLARIS)
/*
 * A replacement for 
 * 			scandir(const char *dir, struct dirent ***namelist, filter, alphasort);
 * because scandir & alphasort don't exist on Solaris 9.
 * Return the dirent->d_name that was sorted the highest according to strcoll, 
 *            or NULL on error or if no entries matched the filter.
 * The caller is responsible for freeing the returned string
 */
char * scan(const char * path) {
	DIR *dir = NULL;
	struct dirent * entry = NULL;
	char * candidate = NULL;
	
	if ((dir = opendir(path)) == NULL) {
		return NULL;
	}

	while ((entry = readdir(dir)) != NULL) {
		if (filter(entry)) {
			if (candidate == NULL) {
				candidate = strdup(entry->d_name);
			} else if (strcoll(candidate, entry->d_name) < 0) {
				free(candidate);
				candidate = strdup(entry->d_name);
			}
		}
	}
	closedir(dir);
	
	return candidate;
}
#endif

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
#if defined(__amd64__) || defined(__x86_64__) || defined(__powerpc64__)
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
			/* try xulrunner-*, mozilla-*, firefox-* directories in /usr/lib/ */
#if defined(__amd64__) || defined(__x86_64__) || defined(__powerpc64__)
			char* dir = "/usr/lib64/";
#else
			char* dir = "/usr/lib/";
#endif
#if defined (SOLARIS)
			char * name = scan(dir);
			if (name != NULL) {
#else
			struct dirent **namelist;
			int i;
			int count = scandir(dir, &namelist, filter, alphasort);
			if (count > 0)
			{
				/* count-1 is used below in an attempt to get the matched directory
				 * with the latest version number.
				 */
				char* name = namelist [count - 1]->d_name;
#endif
				grePath = malloc (strlen(dir) + strlen(name) + 1);
				strcpy(grePath, dir);
				strcat(grePath, name);
#if defined (SOLARIS)
				free(name);
#else
				for (i = 0; i < count; i++) {
					free(namelist [i]);
				}
				free(namelist);
#endif
			}

			if (grePath == NULL)
			{
				/* some other typical installation locations */
				char* dirs[] = {
#if defined(__amd64__) || defined(__x86_64__) || defined(__powerpc64__)
					"/usr/lib64/xulrunner/",
					"/usr/lib64/mozilla-firefox/",
					"/usr/lib64/firefox/",
					"/usr/lib64/mozilla-seamonkey/",
					"/usr/lib64/seamonkey/",
					"/usr/lib64/mozilla/",
#endif
#if defined (SOLARIS)
					"/usr/sfw/lib/xulrunner/",
					"/usr/sfw/lib/mozilla-firefox/",
					"/usr/sfw/lib/firefox/",
					"/usr/sfw/lib/mozilla/",
					"/usr/sfw/lib/mozilla-seamonkey/",
					"/usr/sfw/lib/seamonkey/",
#endif
					"/usr/lib/xulrunner/",
					"/usr/lib/mozilla-firefox/",
					"/usr/lib/firefox/",
					"/usr/lib/mozilla-seamonkey/",
					"/usr/lib/seamonkey/",
					"/usr/lib/mozilla/",
					"/usr/local/xulrunner/",
					"/opt/xulrunner/",
					"/usr/local/mozilla-firefox/",
					"/usr/local/firefox/",
					"/opt/mozilla-firefox/",
					"/opt/firefox/",
					"/usr/local/mozilla-seamonkey/",
					"/usr/local/seamonkey/",
					"/opt/mozilla-seamonkey/",
					"/opt/seamonkey/",
					"/usr/local/mozilla/",
					"/opt/mozilla/",
					NULL
				};

#if defined (SOLARIS)
				/*
				 * The solaris compiler does not do static linking, so just check
				 * for a common lib to ensure that the install seems valid.
				 */
				char* testlib = "libxpcom.so";
#else
				/* Ensure that the install is dynamically-linked and is built with GTK2 */
				char* testlib = "components/libwidget_gtk2.so";
#endif

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
