/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *     Tom Tromey (Red Hat, Inc.)
 *******************************************************************************/

#include "eclipseMozilla.h"
#include "eclipseCommon.h"
#include "eclipseOS.h"
#include "eclipseUtil.h"

#include <signal.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <dlfcn.h>
#ifdef SOLARIS
#include <sys/filio.h>
#endif
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <locale.h>

#include <gtk/gtk.h>
#include <gdk-pixbuf/gdk-pixbuf.h>

/* Global Variables */
char*  consoleVM     = "java";
char*  defaultVM     = "java";
char*  vmLibrary 	 = "libjvm.so";
char*  shippedVMDir  = "jre/bin/";

/* Define the special arguments for the various Java VMs. */
static char*  argVM_JAVA[]        = { NULL };
static char*  argVM_J9[]          = { "-jit", "-mca:1024", "-mco:1024", "-mn:256", "-mo:4096", 
									  "-moi:16384", "-mx:262144", "-ms:16", "-mr:16", NULL };

/* TODO need a better way of doing this*/
#ifdef i386
#define JAVA_ARCH "i386"
#elif defined(__ppc__)
#define JAVA_ARCH "ppc"
#endif

#define MAX_LOCATION_LENGTH 20 /* none of the jvmLocations strings should be longer than this */ 
static const char* jvmLocations [] = { "j9vm",
									   "classic",
									   "../lib/" JAVA_ARCH "/client",  
									   "../lib/" JAVA_ARCH "/server",
								 	   NULL };

/* Define local variables . */
static int          saveArgc   = 0;
static char**       saveArgv   = 0;
static long			splashHandle = 0;

/* Local functions */
static void adjustLibraryPath( char * vmLibrary );
static char * findLib(char * command);

/* Create and Display the Splash Window */
int showSplash( const char* featureImage )
{
	initWindowSystem( &initialArgc, initialArgv, 1);
	
	GdkPixbuf* imageData = NULL;
	GtkWidget* image, *fixed;
  	GtkWindow* main;

    /* Load the feature specific splash image data if defined. */
    if (featureImage != NULL)
    {
    	imageData = gdk_pixbuf_new_from_file(featureImage, NULL);
    }
   
    /* If the splash image data could not be loaded, return an error. */
    if (imageData == NULL)
    	return ENOENT;
    
    /* Create the image from its data. */
    fixed = gtk_fixed_new();
    image = gtk_image_new_from_pixbuf(imageData);

	/* Create a top level window for the image. */
 	main = GTK_WINDOW(gtk_window_new(GTK_WINDOW_TOPLEVEL));
	gtk_window_set_title(main, officialName);

  	gtk_container_add(GTK_CONTAINER(main), GTK_WIDGET(fixed));
  	gtk_container_add(GTK_CONTAINER(fixed), GTK_WIDGET(image));

  	/* Remove window decorations and centre the window on the display. */
  	gtk_window_set_decorated(main, FALSE);
  	gtk_window_set_position(main, GTK_WIN_POS_CENTER);
    gtk_window_set_resizable(main, FALSE);

    /* Set the background pixmap to NULL to avoid a gray flash when the image appears. */
    gtk_widget_realize(GTK_WIDGET(main));
    gdk_window_set_back_pixmap(GTK_WIDGET(main)->window, NULL, FALSE);

	splashHandle = (long)G_OBJECT(fixed);
	/* Show the window and wait for the timeout (or until the process is terminated). */
	gtk_widget_show_all(GTK_WIDGET (main));
	dispatchMessages();
  	return 0;
}

void dispatchMessages() {
	while(g_main_context_iteration(0,0) != 0) {}
}

long getSplashHandle() {
	return splashHandle;
}

void takeDownSplash() {
	if(splashHandle != 0) {
		gtk_widget_destroy((GtkWidget*)splashHandle);
		dispatchMessages();
		splashHandle = 0;
	}
}

/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) 
{
    char** result;
    char*  version;

    if (isJ9VM( vm )) 
        return argVM_J9;
    
    /* Use the default arguments for a standard Java VM */
    result = argVM_JAVA;
    return result;
}

char * findVMLibrary( char* command ) {
	char * lib = findLib(command);
	if( lib != NULL ) {
		adjustLibraryPath(lib);
	}
	return lib;
}
static char * findLib(char * command) {
	int i;
	int pathLength;	
	struct stat stats;
	char * path;				/* path to resulting jvm shared library */
	char * location;			/* points to begining of jvmLocations section of path */
	
	if (command != NULL) {
		location = strrchr( command, dirSeparator ) + 1;
		
		/*check first to see if command already points to the library */
		if (strcmp(location, vmLibrary) == 0) {
			return command;
		}
		
		pathLength = location - command;
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + strlen(vmLibrary) + 1) * sizeof(char));
		strncpy(path, command, pathLength);
		location = &path[pathLength];
		 
		/* 
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		i = -1;
		while(jvmLocations[++i] != NULL) {
			int length = strlen(jvmLocations[i]);			
			strcpy(location, jvmLocations[i]);
			location[length] = dirSeparator;
			location[length + 1] = 0;
			strcat(location, vmLibrary);
			if (stat( path, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return path;
			}
		}
	}
	return NULL;
}

/* adjust the LD_LIBRARY_PATH for the vmLibrary */
static void adjustLibraryPath( char * vmLibrary ) {
	char * buffer;
	char * path;
	char * c;
	char * vmPath;
	char * vmParent;
	char * ldPath;
	char * newPath;
	int vmPathFound = 0;
	int vmParentFound = 0;
	int ldPathLength = 0;
	
#ifdef MOZILLA_FIX
	fixEnvForMozilla();
#endif /* MOZILLA_FIX */

	/* we want the directory containing the library, and the parent directory of that */
	buffer = strdup(vmLibrary);	 
	c = strrchr(buffer, dirSeparator);
	*c = 0;
	vmPath = strdup(buffer);
	
	c = strrchr(buffer, dirSeparator);
	*c = 0;
	vmParent = strdup(buffer);
	free(buffer);
	 
	ldPath = (char*)getenv("LD_LIBRARY_PATH");
	if(!ldPath)
		ldPath = "";
	buffer = malloc((strlen(ldPath) + 2) * sizeof(char));
	strcpy(buffer, ldPath);
	strcat(buffer, ":"); 
	path = buffer;
	while( (c = strchr(path, pathSeparator)) != NULL ) {
		*c++ = 0;
		if( !vmPathFound && strcmp(path, vmPath) == 0 ) {
			vmPathFound = 1;
		} else if( !vmParentFound && strcmp(path, vmParent) == 0 ) {
			vmParentFound = 1;
		} 
		if(vmPathFound && vmParentFound)
			break;
		path = c;
	}	
	free(buffer);
	
	if( vmPathFound && vmParentFound ){
		/*found both on the LD_LIBRARY_PATH, don't need to set it */
		return;
	}
	
	/* set the value for LD_LIBRARY_PATH */
	ldPathLength = strlen(ldPath);
	/* ldPath + separator + vmParth + separator + vmParent + NULL */
	newPath = malloc((ldPathLength + 1 + strlen(vmPath) + 1 + strlen(vmParent) + 1) * sizeof(char));
	strcpy(newPath, vmPath);
	strncat(newPath, &pathSeparator, 1);
	strcat(newPath, vmParent);
	strncat(newPath, &pathSeparator, 1);
	strcat(newPath, ldPath);
	setenv( "LD_LIBRARY_PATH", newPath, 1);
	
	free(vmPath);
	free(vmParent);
	
	/* now we must restart for this to take affect */
	/* TODO what args do we restart with? */
	execv(initialArgv[0], initialArgv);
}
