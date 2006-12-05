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
	GtkAdjustment* vadj, *hadj;
	int width, height;
	GdkPixbuf * pixbuf;
	GtkWidget * image;
	GtkWidget * shellHandle, * vboxHandle, * scrolledHandle, * handle;
	
	initWindowSystem(&initialArgc, initialArgv, 1);
	
	shellHandle = gtk_window_new(GTK_WINDOW_POPUP);
	vboxHandle = gtk_vbox_new(FALSE, 0);
	if(vboxHandle == 0)
		return -1;
		
	vadj = GTK_ADJUSTMENT(gtk_adjustment_new(0, 0, 100, 1, 10, 10));
	hadj = GTK_ADJUSTMENT(gtk_adjustment_new(0, 0, 100, 1, 10, 10));
	if (vadj == 0 || hadj == 0) 
		return -1;
		
	scrolledHandle = gtk_scrolled_window_new(hadj, vadj);
	
	gtk_container_add(GTK_CONTAINER(vboxHandle), scrolledHandle);
	gtk_box_set_child_packing(GTK_BOX(vboxHandle), scrolledHandle, TRUE, TRUE, 0, GTK_PACK_END);
	gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolledHandle), GTK_POLICY_NEVER, GTK_POLICY_NEVER);
	gtk_widget_show(scrolledHandle);
	
	handle = gtk_fixed_new();
	gtk_fixed_set_has_window(GTK_FIXED(handle), TRUE);
	GTK_WIDGET_SET_FLAGS(handle, GTK_CAN_FOCUS);
	
	/* TODO Avoid Warnings */
	gtk_container_add(GTK_CONTAINER(scrolledHandle), handle);
	gtk_widget_show(handle);
	
	gtk_container_add(GTK_CONTAINER(shellHandle), vboxHandle);
	
	pixbuf = gdk_pixbuf_new_from_file(featureImage, NULL);
	image = gtk_image_new_from_pixbuf(pixbuf);
	gtk_container_add(GTK_CONTAINER(handle), image);
	gtk_widget_show(image);
	
	width  = gdk_pixbuf_get_width(pixbuf);
	height = gdk_pixbuf_get_height(pixbuf);
	gtk_window_resize(GTK_WINDOW(shellHandle), width, height);
	gtk_window_set_position(GTK_WINDOW(shellHandle), GTK_WIN_POS_CENTER);
	gtk_widget_show(shellHandle);
	gtk_widget_show_all(GTK_WIDGET(shellHandle));
	splashHandle = (long)G_OBJECT(shellHandle);
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
	/* ldPath + separator + vmPath + separator + vmParent + NULL */
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

void restartLauncher( char* program, char* args[] ) 
{
	/* just restart in-place */
	execv(program, args);
}

