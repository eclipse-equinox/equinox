/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
#include "eclipseGtk.h"

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
	
	if( initWindowSystem(&initialArgc, initialArgv, 1) != 0)
		return -1;
	
	shellHandle = gtk.gtk_window_new(GTK_WINDOW_TOPLEVEL);
	gtk.gtk_window_set_decorated((GtkWindow*)(shellHandle), FALSE);
	gtk.gtk_signal_connect((GtkObject*)shellHandle, "destroy", (GtkSignalFunc)(gtk.gtk_widget_destroyed), &shellHandle);
	vboxHandle = gtk.gtk_vbox_new(FALSE, 0);
	if(vboxHandle == 0)
		return -1;
		
	vadj = (GtkAdjustment*)gtk.gtk_adjustment_new(0, 0, 100, 1, 10, 10);
	hadj = (GtkAdjustment*)gtk.gtk_adjustment_new(0, 0, 100, 1, 10, 10);
	if (vadj == 0 || hadj == 0) 
		return -1;
		
	scrolledHandle = gtk.gtk_scrolled_window_new(hadj, vadj);
	
	gtk.gtk_container_add((GtkContainer*)(vboxHandle), scrolledHandle);
	gtk.gtk_box_set_child_packing((GtkBox*)(vboxHandle), scrolledHandle, TRUE, TRUE, 0, GTK_PACK_END);
	gtk.gtk_scrolled_window_set_policy((GtkScrolledWindow*)(scrolledHandle), GTK_POLICY_NEVER, GTK_POLICY_NEVER);

	handle = gtk.gtk_fixed_new();
	gtk.gtk_fixed_set_has_window((GtkFixed*)(handle), TRUE);
	((GtkObject*)handle)->flags |= GTK_CAN_FOCUS;	/*GTK_WIDGET_SET_FLAGS(handle, GTK_CAN_FOCUS);*/
	
	/* avoid gtk_scrolled_window_add warning */
	handlerId = gtk.g_log_set_handler("Gtk", G_LOG_LEVEL_WARNING, &log_handler, NULL);
	gtk.gtk_container_add((GtkContainer*)(scrolledHandle), handle);
	gtk.g_log_remove_handler("Gtk", handlerId);
	
	gtk.gtk_container_add((GtkContainer*)(shellHandle), vboxHandle);
	
	pixbuf = gtk.gdk_pixbuf_new_from_file(featureImage, NULL);
	image = gtk.gtk_image_new_from_pixbuf(pixbuf);
	gtk.gtk_signal_connect((GtkObject*)(image), "destroy", (GtkSignalFunc)(gtk.gtk_widget_destroyed), &image);
	gtk.gtk_container_add((GtkContainer*)(handle), image);
	
	width  = gtk.gdk_pixbuf_get_width(pixbuf);
	height = gtk.gdk_pixbuf_get_height(pixbuf);
	gtk.gtk_window_set_position((GtkWindow*)(shellHandle), GTK_WIN_POS_CENTER);
	if (getOfficialName() != NULL)
		gtk.gtk_window_set_title((GtkWindow*)(shellHandle), getOfficialName());
	gtk.gtk_window_resize((GtkWindow*)(shellHandle), width, height);
	gtk.gtk_widget_show_all((GtkWidget*)(shellHandle));
	splashHandle = (long)shellHandle;
	dispatchMessages();
	return 0;
}

void dispatchMessages() {
	if (gtk.g_main_context_iteration != 0)
		while(gtk.g_main_context_iteration(0,0) != 0) {}
}

jlong getSplashHandle() {
	return splashHandle;
}

void takeDownSplash() {
	if(shellHandle != 0) {
		gtk.gtk_widget_destroy(shellHandle);
		if (image != NULL) {
			gtk.gtk_widget_destroy(image);
			gtk.g_object_unref(pixbuf);
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

JavaResults* launchJavaVM( char* args[] )
{
	JavaResults* jvmResults = NULL;
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
      	/* TODO, how to distinguish this as a launch problem to the other process? */
      	_exit(errno);
    }

  	jvmResults = malloc(sizeof(JavaResults));
  	memset(jvmResults, 0, sizeof(JavaResults));
  	
	/* If the JVM is still running, wait for it to terminate. */
	if (jvmProcess != 0)
	{
		waitpid(jvmProcess, &exitCode, 0);
      	if (WIFEXITED(exitCode))
      		/* TODO, this should really be a runResult if we could distinguish the launch problem above */
			jvmResults->launchResult = WEXITSTATUS(exitCode);
    }

	return jvmResults;
}
