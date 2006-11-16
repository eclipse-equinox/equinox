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
char*  shippedVMDir  = "jre/bin/";

/* Define the special arguments for the various Java VMs. */
static char*  argVM_JAVA[]        = { NULL };
static char*  argVM_J9[]          = { "-jit", "-mca:1024", "-mco:1024", "-mn:256", "-mo:4096", 
									  "-moi:16384", "-mx:262144", "-ms:16", "-mr:16", NULL };


/* Define local variables . */
static int          saveArgc   = 0;
static char**       saveArgv   = 0;

/* Local functions */
static gboolean splashTimeout(gpointer data);

/* Create and Display the Splash Window */
int showSplash( char* timeoutString, char* featureImage )
{
	GdkPixbuf* imageData = NULL;
	GtkWidget* image, *fixed;
  	GtkWindow* main;
  	int        timeout = 0;

	/* Determine the splash timeout value (in seconds). */
	if (timeoutString != NULL && strlen( timeoutString ) > 0)
	{
	    sscanf( timeoutString, "%d", &timeout );
	}

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

	/* If a timeout for the splash window was given */
	if (timeout != 0)
	{
		/* Add a timeout (in milliseconds) to bring down the splash screen. */
    	gtk_timeout_add((timeout * 1000), splashTimeout, (gpointer) main);
	}

	/* Show the window and wait for the timeout (or until the process is terminated). */
	gtk_widget_show_all(GTK_WIDGET (main));
	gtk_main ();
  	return 0;
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

/* Splash Timeout - Hide the main window and exit the main loop. */
static gboolean splashTimeout(gpointer data)
{
	GtkWidget* main = GTK_WIDGET(data);
  	gtk_widget_hide(main);
  	gtk_main_quit();
  	return FALSE;
}

char * findVMLibrary( _TCHAR * command ) {
	return NULL;
}