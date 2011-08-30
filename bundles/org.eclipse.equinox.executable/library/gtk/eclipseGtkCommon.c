/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
#include "eclipseGtk.h"

#include <locale.h>
#include <dlfcn.h>
#include <stdio.h>

#define ECLIPSE_ICON  401

char   dirSeparator  = '/';
char   pathSeparator = ':';

/* Define local variables for the main window. */
static int          saveArgc   = 0;		/* arguments after they were parsed, for window system */
static char**       saveArgv   = 0;

gboolean     gtkInitialized = FALSE;

#ifdef SOLARIS
/* a call to this function appears inline in glib/gstring.h on Solaris,
   so provide a definition here and hook it up
   */
GString* g_string_insert_c (GString *string, gssize pos, gchar c) {
	/* see bug 264615, we can get here without having initialized the gtk pointers */
	if (gtk.not_initialized)
		loadGtk();
	return gtk.g_string_insert_c(string, pos, c);
}
#endif

/* Display a Message */
void displayMessage(char* title, char* message)
{
	GtkWidget* dialog;
	
    /* If GTK has not been initialized yet, do it now. */
    if (initWindowSystem( &saveArgc, saveArgv, 1 ) != 0) {
    	printf("%s:\n%s\n", title, message);
    	return;
    }

  	dialog = gtk.gtk_message_dialog_new(NULL, GTK_DIALOG_DESTROY_WITH_PARENT,
				   					GTK_MESSAGE_ERROR, GTK_BUTTONS_CLOSE,
				   					"%s", message);
  	gtk.gtk_window_set_title((GtkWindow*)dialog, title);
  	gtk.gtk_dialog_run((GtkDialog*)dialog);
  	gtk.gtk_widget_destroy(dialog);
}

/* Initialize the Window System */
int initWindowSystem(int* pArgc, char* argv[], int showSplash)
{
	int defaultArgc = 1;
	char * defaultArgv [] = { "", 0 };
	
    if(gtkInitialized)
    	return 0;
    
    /* load the GTK libraries and initialize function pointers */
    if (loadGtk() != 0)
    	return -1;
    
    if (getOfficialName() != NULL) 
    	defaultArgv[0] = getOfficialName();
    
	if (argv == NULL) {
		/* gtk_init_check on Solaris 9 doesn't like NULL or empty argv */
		pArgc = &defaultArgc;
		argv = defaultArgv;
	}
	
    /* Save the arguments in case displayMessage() is called in the main launcher. */ 
    if (saveArgv == 0)
    {
    	saveArgc = *pArgc;
    	saveArgv =  argv;
    }  

	/* Initialize GTK. */
    gtk.gtk_set_locale();
    if (!gtk.gtk_init_check(pArgc, &argv)) {
    	return -1;
    }

	/*_gdk_set_program_class(getOfficialName());*/
	gtkInitialized = TRUE;
	return 0;
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

