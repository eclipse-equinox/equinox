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
#include "eclipseOS.h"

#include <locale.h>
#include <dlfcn.h>
#include <gtk/gtk.h>

#define ECLIPSE_ICON  401

char   dirSeparator  = '/';
char   pathSeparator = ':';

void initWindowSystem( int* pArgc, _TCHAR* argv[], int showSplash );

/* Global Main Window*/
/*#ifdef UNICODE
extern HWND topWindow;
#else
HWND    topWindow = 0;
#endif*/

/* Define local variables for the main window. */
static int          saveArgc   = 0;		/* arguments after they were parsed, for window system */
static char**       saveArgv   = 0;

gboolean     gtkInitialized = FALSE;

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
    if(gtkInitialized)
    	return;
    /* Save the arguments in case displayMessage() is called in the main launcher. */ 
    if (saveArgv == 0)
    {
    	saveArgc = *pArgc;
    	saveArgv =  argv;
    }  

	/* Initialize GTK. */
	gtk_set_locale();
	gtk_init(pArgc, &argv);
	gdk_set_program_class(officialName);
	gtkInitialized = TRUE;
}

/* Load the specified shared library
 */
void * loadLibrary( char * library ){
	void * result= dlopen(library, RTLD_LAZY);
	if(result == 0) 
		printf("%s\n",dlerror());
	return result;
}

/* Unload the shared library
 */
void unloadLibrary( void * handle ){
	dlclose(handle);
}
 
/* Find the given symbol in the shared library
 */
void * findSymbol( void * handle, char * symbol ){
	return dlsym(handle, symbol);
}

