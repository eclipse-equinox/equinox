/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at 
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *     Tom Tromey (Red Hat, Inc.)
 *******************************************************************************/

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

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <locale.h>
#include <semaphore.h>
#include <fcntl.h>

/* Global Variables */
char*  defaultVM     = "java";
char*  vmLibrary 	 = "libjvm.so";
char*  shippedVMDir  = "jre/bin/";

/* Define the special arguments for the various Java VMs. */
static char*  argVM_JAVA[]        = { NULL };


/* Define local variables . */
static GtkWidget*	splashHandle = 0;
static GtkWidget*   shellHandle = 0;

static _TCHAR** openFilePath = NULL; /* the files we want to open */
static int openFileTimeout = 60; 	 /* number of seconds to wait before timeout */
static int filesPassedToSWT = 0;	 /* set to 1 on success */
static const int FILEOPEN_RETRY_TIMEOUT_MS = 1000;

/** GDBus related */
static const gchar GDBUS_SERVICE[] = "org.eclipse.swt";
static const gchar GDBUS_OBJECT[] = "/org/eclipse/swt";
static const gchar GDBUS_INTERFACE[] = "org.eclipse.swt";
GDBusProxy *gdbus_proxy = NULL;

gboolean gdbus_initProxy ();
gboolean gdbus_testConnection();
gboolean gdbus_FileOpen_TimerProc(gpointer data);
gboolean gdbus_call_FileOpen ();

/*
 * Deals with opening files passed to eclipse.  e.g: ./eclipse /myfile
 *
 * return 1 = Files passed to eclipse. Don't spawn another instance.
 *        0 = Launch a new eclipse instance, will try to pass files to eclipse once instance is launched.
 */
gboolean reuseWorkbench(_TCHAR** filePath, int timeout) {
	openFileTimeout = timeout;
	openFilePath = filePath;

	if (initWindowSystem(&initialArgc, initialArgv, 1) != 0)
		return -1;

	if (!gdbus_initProxy()) {
		_ftprintf(stderr, "Launcher Error. Could not init gdbus proxy. Bug? Launching eclipse without opening files passed in.\n");
		return  0;
	}

	// If eclipse already open, just pass files.
	if (gdbus_testConnection()) {
		return gdbus_call_FileOpen();
	} else {
		// Otherwise add a timer that will keep trying to pass files to eclipse for a few minutes until it succeeds or times out.
		// Note, the while loop in launchJavaVM() ensures the launcher doesn't quit before the timer expired.
		gtk.g_timeout_add(FILEOPEN_RETRY_TIMEOUT_MS, gdbus_FileOpen_TimerProc, 0);
		return 0;
	}
}

/**
 * Initializes variables/structures for dbus connectivity to GDBus session.
 * Can be called multiple times, only first time initializes, other times just return early (1).
 *
 * DO NOT USE TO TEST CONNECTION. Use dbus_testConnection() instead.
 *
 * return: 0 (false) bug, something that shouldn't fail failed. This can happen if dynamic function calls failed or gdbus is not available etc..
 *         1 (true) proxy configured.
 */
gboolean gdbus_initProxy () {
	if (gdbus_proxy != NULL)
		return 1; // already initialized.

	// Construct service name: org.eclipse.swt.<name>
	const gint serviceNameLength = strlen(GDBUS_SERVICE) + strlen(getOfficialName()) + 2;
	gchar *serviceName = (gchar *) malloc(serviceNameLength * sizeof(gchar));
	snprintf(serviceName, serviceNameLength, "%s.%s", GDBUS_SERVICE, getOfficialName());

	// Function 'g_type_init()' is not needed anymore as of glib 2.36 as gtype system is initialized earlier. It is marked as deprecated.
	// It is here because at the time of writing, eclipse supports glib 2.28.
	// It is dynamic to prevent compile warning. But should be removed once min glib eclipse version >= 2.36
	gtk.g_type_init();

	GError *error = NULL; // Some functions return errors through params

	gdbus_proxy = gtk.g_dbus_proxy_new_for_bus_sync(G_BUS_TYPE_SESSION, G_DBUS_PROXY_FLAGS_NONE, NULL, serviceName, GDBUS_OBJECT, GDBUS_INTERFACE, NULL, &error);
	if ((gdbus_proxy == NULL) || (error != NULL)) {
		fprintf(stderr, "Launcher error: GDBus proxy init failed to connect %s:%s on %s.\n", serviceName, GDBUS_OBJECT, GDBUS_INTERFACE);
		if (error != NULL) {
			_ftprintf(stderr, "Launcher error: GDBus gdbus_proxy init failed for reason: %s\n", error->message);
			gtk.g_error_free (error);
		}
		free(serviceName);
		return 0;
	} else {
		free(serviceName);
		return 1;
	}
}

/*
 * Test if we can reach org.eclipse.swt dbus session.
 *
 * return 0 (false) No connection.
 *        1 (true)  org.eclipse.swt listens to calls.
 */
gboolean gdbus_testConnection() {
	if (!gdbus_initProxy())
		return 0;
	GError *error = NULL; // Some functions return errors through params
	GVariant *result;     // The value result from a call
	result = gtk.g_dbus_proxy_call_sync(gdbus_proxy, "org.freedesktop.DBus.Peer.Ping", 0, G_DBUS_CALL_FLAGS_NONE, /* Proxy default timeout */ -1, NULL, &error);
	if (error != NULL) {
			gtk.g_error_free(error);
			return 0;
	}
	if (result != NULL) {
		gtk.g_variant_unref (result);
		return 1;
	}
	_ftprintf(stderr, "ERROR: testConnection failed due to unknown reason. Bug in eclipseGtk.c? Potential cause could be dynamic function not initialized properly\n");
	return 0;
}

/*
 * Timer callback function.
 *
 * Try to pass files to eclipse. If eclipse not up yet, try again later.
 *
 * Timer ends when it returns false. (Files passed to eclipse or timeout).
 */
gboolean gdbus_FileOpen_TimerProc(gpointer data) {
	if (openFileTimeout == 0)
		return 0; // stop timer.
	openFileTimeout--;
	if (gdbus_testConnection()) {
		gdbus_call_FileOpen();
		filesPassedToSWT = 1;
		return 0; // stop timer.
	}
	return 1; // run timer again.
}

/*
 * Call fileOpen method in SWT.  Note, in SWT, see GDBus.java. fileOpen GDBusMethod is defined in Display.java.
 * This call can be called multiple times if Eclipse hasn't launched yet.
 *
 * Return: FALSE (0) Call did not work. Probably eclipse not fired up yet. (try again later)
 *         TRUE (1) GDBus call completed successfully.
 */
gboolean gdbus_call_FileOpen () {
	if (!gdbus_initProxy())
		return 0;

	// Construct GDBus arguments based on files passed into launcher.
	GVariantBuilder *builder;
	GVariant *paramaters;
	builder = gtk.g_variant_builder_new ((const GVariantType *) "as");  // as = G_VARIANT_TYPE_STRING_ARRAY

	int i = -1;
	while (openFilePath[++i] != NULL) {
			gtk.g_variant_builder_add (builder, (const gchar *) (const GVariantType *) "s", (const gchar *) openFilePath[i]);  // s = G_VARIANT_TYPE_STRING
	}

	paramaters = gtk.g_variant_new ("(as)", builder);
	gtk.g_variant_builder_unref (builder);

	// Send a message
	GError *error = NULL;
	GVariant *result;
	result = gtk.g_dbus_proxy_call_sync(gdbus_proxy, "FileOpen", paramaters, G_DBUS_CALL_FLAGS_NONE, /* Proxy default timeout */ -1, NULL, &error);
	if (error != NULL) {
		gtk.g_error_free (error);
		return 0; // did not work. Eclipse probably not up yet. Try again later.
	} else {
		 if (result != NULL) {
			// Because this not straight forward, below is an example of how to retrieve string return value if needed in the future.
			// Note, arguments are packaged into a tuple because we deal with gdbus in dynamic way.
			// gchar *str;
			// g_variant_get(result, "(&s)", &str);
			 gtk.g_variant_unref(result);
		}
		return 1; // worked.
	}
}

/* Get current scaling-factor */
float scaleFactor () {
	float scaleFactor = 1;
	GdkScreen * screen;
	double resolution;
	screen = gtk.gdk_screen_get_default();
	resolution = gtk.gdk_screen_get_resolution (screen);
	if (resolution <= 0) resolution = 96; // in unix and windows 100% corresponds to dpi of 96
	resolution = ((int)((resolution + 24) / 96)) * 96; //rounding the resolution to 100% multiples,this implementation needs to be kept in sync with org.eclipse.swt.internal.DPIUtil#setDeviceZoom(int)
	scaleFactor = (float)(resolution / 96);
	return scaleFactor;
}

/* Create and Display the Splash Window */
int showSplash( const char* featureImage ) {
	GtkWidget *image;
	GdkPixbuf *pixbuf, *scaledPixbuf;
	int width, height;
	float scalingFactor;

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
	gtk.gtk_window_set_type_hint((GtkWindow*)(shellHandle), 4 /*GDK_WINDOW_TYPE_HINT_SPLASHSCREEN*/);
	gtk.g_signal_connect_data((gpointer)shellHandle, "destroy", (GCallback)(gtk.gtk_widget_destroyed), &shellHandle, NULL, 0);
		
	pixbuf = gtk.gdk_pixbuf_new_from_file(featureImage, NULL);
	width = gtk.gdk_pixbuf_get_width(pixbuf);
	height = gtk.gdk_pixbuf_get_height(pixbuf);
	scalingFactor = scaleFactor();

	if (scalingFactor > 1) {
		scaledPixbuf = gtk.gdk_pixbuf_scale_simple(pixbuf, width * scalingFactor, height * scalingFactor, GDK_INTERP_BILINEAR);
	} else {
		scaledPixbuf = pixbuf;
	}
	
	image = gtk.gtk_image_new_from_pixbuf(scaledPixbuf);
	if (pixbuf) {
		gtk.g_object_unref(pixbuf);
	}
	gtk.gtk_container_add((GtkContainer*)(shellHandle), image);
	
	if (getOfficialName() != NULL)
		gtk.gtk_window_set_title((GtkWindow*)(shellHandle), getOfficialName());
	gtk.gtk_window_set_position((GtkWindow*)(shellHandle), GTK_WIN_POS_CENTER);
	gtk.gtk_window_resize((GtkWindow*)(shellHandle), gtk.gdk_pixbuf_get_width(scaledPixbuf), gtk.gdk_pixbuf_get_height(scaledPixbuf));
	gtk.gtk_widget_show_all((GtkWidget*)(shellHandle));
	splashHandle = shellHandle;
	dispatchMessages();
	return 0;
}

void dispatchMessages() {
	if (gtk.g_main_context_iteration != 0)
		while(gtk.g_main_context_iteration(0,0) != 0) {}
}

jlong getSplashHandle() {
	return (jlong) splashHandle;
}

void takeDownSplash() {
	if(shellHandle != 0) {
		gtk.gtk_widget_destroy(shellHandle);
		dispatchMessages();
		splashHandle = 0;
		shellHandle = NULL;
	}
}

/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) {
    char** result;

/*    if (isJ9VM( vm )) 
        return argVM_J9;*/
    
    /* Use the default arguments for a standard Java VM */
    result = argVM_JAVA;
    return result;
}

JavaResults* launchJavaVM( char* args[] ) {
	JavaResults* jvmResults = NULL;
  	pid_t   jvmProcess, finishedProcess = 0;
  	int     exitCode;
  	
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
		/* When attempting a file open, we need to spin the event loop
		 * for setAppWindowTimerProc to run.  When that succeeds or times out, 
		 * we can stop the event loop and just wait on the child process.
		 */
		if (openFilePath != NULL) {
			struct timespec sleepTime;
			sleepTime.tv_sec = 0;
			sleepTime.tv_nsec = 5e+8; // 500 milliseconds
			
			// Ensure we don't quit the launcher until gdbus_FileOpen_TimerProc() finished or timed out.
			// If making any changes to this loop, ensure "./eclipse /myFile" still works.
			while(openFileTimeout > 0 && !filesPassedToSWT && (finishedProcess = waitpid(jvmProcess, &exitCode, WNOHANG)) == 0) {
				dispatchMessages();
				nanosleep(&sleepTime, NULL);
			}
		}
		if (finishedProcess == 0)
			waitpid(jvmProcess, &exitCode, 0);
      	if (WIFEXITED(exitCode))
      		/* TODO, this should really be a runResult if we could distinguish the launch problem above */
			jvmResults->launchResult = WEXITSTATUS(exitCode);
    }
	return jvmResults;
}
