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
char*  defaultVM     = "java";
char*  vmLibrary 	 = "libjvm.so";
char*  shippedVMDir  = "jre/bin/";

/* Define the special arguments for the various Java VMs. */
static char*  argVM_JAVA[]        = { NULL };

/* Define local variables . */
static long			splashHandle = 0;
static GtkWidget*   shellHandle = 0;
static GdkPixbuf*	pixbuf = 0;
static GtkWidget*   image = 0;

/* Local functions */
static void log_handler(const gchar* domain, GLogLevelFlags flags, const gchar* msg, gpointer data) {
	/* nothing */
}
/* Create and Display the Splash Window */
int showSplash( const char* featureImage )
{
	GtkAdjustment* vadj, *hadj;
	int width, height;
	guint handlerId;
	GtkWidget* vboxHandle, * scrolledHandle, * handle;

	if (splashHandle != 0)
		return 0; /* already showing splash */
	if (featureImage == NULL)
		return -1;
	
	if (initialArgv == NULL)
		initialArgc = 0;
	initWindowSystem(&initialArgc, initialArgv, 1);
	
	shellHandle = gtk_window_new(GTK_WINDOW_TOPLEVEL);
	gtk_window_set_decorated(GTK_WINDOW(shellHandle), FALSE);
	gtk_signal_connect(GTK_OBJECT(shellHandle), "destroy", GTK_SIGNAL_FUNC(gtk_widget_destroyed), &shellHandle);
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
	
	/* avoid gtk_scrolled_window_add warning */
	handlerId = g_log_set_handler("Gtk", G_LOG_LEVEL_WARNING, &log_handler, NULL);
	gtk_container_add(GTK_CONTAINER(scrolledHandle), handle);
	g_log_remove_handler("Gtk", handlerId);
	
	gtk_widget_show(handle);
	
	gtk_container_add(GTK_CONTAINER(shellHandle), vboxHandle);
	
	pixbuf = gdk_pixbuf_new_from_file(featureImage, NULL);
	image = gtk_image_new_from_pixbuf(pixbuf);
	gtk_signal_connect(GTK_OBJECT(image), "destroy", GTK_SIGNAL_FUNC(gtk_widget_destroyed), &image);
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
	if(shellHandle != 0) {
		gtk_widget_destroy(shellHandle);
		if (image != NULL) {
			gtk_widget_destroy(image);
			gdk_pixbuf_unref(pixbuf);
		}
		dispatchMessages();
		splashHandle = 0;
		shellHandle = NULL;
	}
}

/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) 
{
    char** result;

/*    if (isJ9VM( vm )) 
        return argVM_J9;*/
    
    /* Use the default arguments for a standard Java VM */
    result = argVM_JAVA;
    return result;
}

int launchJavaVM( char* args[] )
{
	int     jvmExitCode = 1;
  	pid_t   jvmProcess;
  	int     exitCode;
  	
#ifdef MOZILLA_FIX
	fixEnvForMozilla();
#endif /* MOZILLA_FIX */
	
	jvmProcess = fork();
  	if (jvmProcess == 0) 
    {
    	/* Child process ... start the JVM */
      	execv(args[0], args);

      	/* The JVM would not start ... return error code to parent process. */
      	_exit(errno);
    }

	/* If the JVM is still running, wait for it to terminate. */
	if (jvmProcess != 0)
	{
		wait(&exitCode);
      	if (WIFEXITED(exitCode))
			jvmExitCode = WEXITSTATUS(exitCode);
    }

	return jvmExitCode;
}
