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
#include "eclipseOS.h"
#include "eclipseUtil.h"

#include <signal.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
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
char   dirSeparator  = '/';
char   pathSeparator = ':';
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
static gboolean     gtkInitialized = FALSE;

static GtkWidget *label = NULL, *progress = NULL;
static GdkColor foreground = {0, 0, 0, 0};
static GdkRectangle progressRect = {0, 0, 0, 0}, messageRect = {0, 0, 0, 0};
static int value = 0, maximum = 100;

/* Local functions */
static gboolean splashTimeout(gpointer data);

/* Display a Message */
void displayMessage(char* title, char* message)
{
	GtkWidget* dialog;
	
    /* If GTK has not been initialized yet, do it now. */
    if (!gtkInitialized) 
    {
		initWindowSystem( &saveArgc, saveArgv, 1 );
    }

  	dialog = gtk_message_dialog_new(NULL, GTK_DIALOG_DESTROY_WITH_PARENT,
				   					GTK_MESSAGE_ERROR, GTK_BUTTONS_CLOSE,
				   					"%s", message);
  	gtk_window_set_title(GTK_WINDOW (dialog), title);
  	gtk_dialog_run(GTK_DIALOG (dialog));
  	gtk_widget_destroy(dialog);
}


/* Initialize the Window System */
void initWindowSystem(int* pArgc, char* argv[], int showSplash)
{
    
    /* Save the arguments in case displayMessage() is called in the main launcher. */ 
    if (saveArgv == 0)
    {
    	saveArgc = *pArgc;
    	saveArgv =  argv;
    }  

    
    /* If the splash screen is going to be displayed by this process */
    if (showSplash)
    {
    	/* Initialize GTK. */
  		gtk_set_locale();
  		gtk_init(pArgc, &argv);
  		gdk_set_program_class(officialName);
  		gtkInitialized = TRUE;
	}
}

static void readRect(char *str, GdkRectangle *rect) {
	int x, y, width, height;
	char *temp = str, *comma;
	comma = strchr(temp, ',');
	if (comma == NULL) return;
	comma[0] = 0;
	x = atoi(temp);
	temp = comma + 1;
	comma = strchr(temp, ',');
	if (comma == NULL) return;
	comma[0] = 0;
	y = atoi(temp);
	temp = comma + 1;
	comma = strchr(temp, ',');
	if (comma == NULL) return;
	comma[0] = 0;
	width = atoi(temp);
	temp = comma + 1;
	height = atoi(temp);
	rect->x = x;
	rect->y = y;
	rect->width = width;
	rect->height = height;
}

static void readColor(char *str, GdkColor *color) {
	int value = atoi(str);
	color->red = ((value & 0xFF0000) >> 16) * 0xFF;
	color->green = ((value & 0xFF00) >> 8) * 0xFF;
	color->blue = ((value & 0xFF) >> 0) * 0xFF;
}

static void readInput() {
	int available;
	FILE *fd = stdin;
	char *buffer = NULL, *equals = NULL, *end, *line;
	ioctl(fileno(fd), FIONREAD, &available);
	if (available <= 0) return;
	buffer = malloc(available + 1);
	available = fread(buffer, 1, available, fd);
	buffer[available] = 0;
	line = buffer;
	while (line != NULL) {
		end = strchr(line, '\n');
		equals = strchr(line, '=');
		if (end != NULL) end[0] = 0;
		if (equals != NULL) {
			char *str = (char *)equals + 1;
			equals[0] = 0;
			if (strcmp(line, "maximum") == 0) {
				maximum = atoi(str);
				if (progress) {
					double fraction = maximum == 0 ? 1 : (double)(value / maximum);
					gtk_progress_bar_set_fraction (GTK_PROGRESS_BAR(progress), fraction);
				}
			} else if (strcmp(line, "value") == 0) {
				value = atoi(str);
				if (progress) {
					double fraction = maximum == 0 ? 1 : (double)value / maximum;
					gtk_progress_bar_set_fraction (GTK_PROGRESS_BAR(progress), fraction);
				}
			} else if (strcmp(line, "progressRect") == 0) {
				readRect(str, &progressRect);
				if (progress) {
					gtk_fixed_move(GTK_FIXED(gtk_widget_get_parent(progress)), progress, progressRect.x, progressRect.y);
				  	gtk_widget_set_size_request(GTK_WIDGET(progress), progressRect.width, progressRect.height);
				}
			} else if (strcmp(line, "messageRect") == 0) {
				readRect(str, &messageRect);
				if (label) {
					gtk_fixed_move(GTK_FIXED(gtk_widget_get_parent(label)), label, messageRect.x, messageRect.y);
				  	gtk_widget_set_size_request(GTK_WIDGET(label), messageRect.width, messageRect.height);
				}
			} else if (strcmp(line, "foreground") == 0) {
				readColor(str, &foreground);
				if (label) {
					gtk_widget_modify_fg (label, GTK_STATE_NORMAL, &foreground);
				}
			} else if (strcmp(line, "message") == 0) {
				if (label) {
					gtk_label_set_text(GTK_LABEL(label), str);
				}
			}
			
		}
		if (end != NULL) line = end + 1;
		else line = NULL;
	}
	free(buffer);
}

static gboolean timerProc(gpointer data) {
	readInput();
  	return TRUE;
}

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
  	label = gtk_label_new("");
  	progress = gtk_progress_bar_new();

	/* Create a top level window for the image. */
 	main = GTK_WINDOW(gtk_window_new(GTK_WINDOW_TOPLEVEL));
	gtk_window_set_title(main, officialName);

  	gtk_container_add(GTK_CONTAINER(main), GTK_WIDGET(fixed));
  	gtk_container_add(GTK_CONTAINER(fixed), GTK_WIDGET(image));
  	gtk_container_add(GTK_CONTAINER(fixed), GTK_WIDGET(label));
  	gtk_container_add(GTK_CONTAINER(fixed), GTK_WIDGET(progress));

	gtk_misc_set_alignment (GTK_MISC(label), 0.0f, 0.0f);
	gtk_label_set_justify (GTK_LABEL(label), GTK_JUSTIFY_LEFT);
	gtk_widget_modify_fg (label, GTK_STATE_NORMAL, &foreground);

    readInput();
 	gtk_timeout_add(50, timerProc, NULL);

	gtk_fixed_move(GTK_FIXED(fixed), label, messageRect.x, messageRect.y);
  	gtk_widget_set_size_request(GTK_WIDGET(label), messageRect.width, messageRect.height);
	gtk_fixed_move(GTK_FIXED(fixed), progress, progressRect.x, progressRect.y);
  	gtk_widget_set_size_request(GTK_WIDGET(progress), progressRect.width, progressRect.height);
  	gtk_widget_set_size_request(GTK_WIDGET(fixed), gdk_pixbuf_get_width(imageData), gdk_pixbuf_get_height(imageData));

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

	label = progress = NULL;

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


/* Start the Java VM 
 *
 * This method is called to start the Java virtual machine and to wait until it
 * terminates. The function returns the exit code from the JVM.
 */
int startJavaVM( char* args[] ) 
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

/* Splash Timeout - Hide the main window and exit the main loop. */
static gboolean splashTimeout(gpointer data)
{
	GtkWidget* main = GTK_WIDGET(data);
  	gtk_widget_hide(main);
  	gtk_main_quit();
  	return FALSE;
}
