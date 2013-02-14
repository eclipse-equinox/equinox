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
#include "eclipseMotif.h"

#include <locale.h>
#include <dlfcn.h>
#include <stdlib.h>

#define ECLIPSE_ICON  401

char   dirSeparator  = '/';
char   pathSeparator = ':';

void centreShell( Widget widget, Widget expose );

/* Global Variables */
XtAppContext appContext = 0;
Widget       topWindow  = 0;

/* Define local variables for the main window. */
static int          saveArgc   = 0;		/* arguments after they were parsed, for window system */
static char**       saveArgv   = 0;

int motifInitialized = 0;

/* Display a Message */
void displayMessage( char* title, char* message ) 
{
    char*         displayName = NULL;
    Widget        msgBox = NULL;
    XmString      msg;
    Arg           arg[20];
    int           nArgs;
    XEvent        event;
    
    /* If there is no associated display, or we fail to initialize Xt, just print the error and return. */
    displayName = getenv("DISPLAY");
    if ( displayName == NULL || strlen(displayName) == 0 ||
        (topWindow == 0 && initWindowSystem( &saveArgc, saveArgv, 1 ) != 0)	) 
    {
    	printf("%s:\n%s\n", title, message);
    	return;
    }
	msg = motif.XmStringGenerate( message, NULL, XmCHARSET_TEXT, NULL );

    /* Output a simple message box. */ 
    nArgs = 0;
    
    motif_XtSetArg( arg[ nArgs ], XmNdialogType, XmDIALOG_MESSAGE ); nArgs++;
    motif_XtSetArg( arg[ nArgs ], XmNtitle, title ); nArgs++;
    motif_XtSetArg( arg[ nArgs ], XmNmessageString, msg ); nArgs++;
    msgBox = motif.XmCreateMessageDialog( topWindow, getOfficialName(), arg, nArgs );

    motif.XtUnmanageChild( motif.XmMessageBoxGetChild( msgBox, XmDIALOG_CANCEL_BUTTON ) );
    motif.XtUnmanageChild( motif.XmMessageBoxGetChild( msgBox, XmDIALOG_HELP_BUTTON ) );
    motif.XtManageChild( msgBox );
    centreShell( msgBox, msgBox );
    if (msg != 0) motif.XmStringFree (msg);

    /* Wait for the OK button to be pressed. */
    while (motif_XtIsRealized( msgBox ) && motif.XtIsManaged( msgBox ))
    {
        motif.XtAppNextEvent( appContext, &event );
        motif.XtDispatchEvent( &event );
    }
    motif.XtDestroyWidget( msgBox );
}

/* Initialize Window System
 *
 * Initialize the Xt and Xlib.
 */
int initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
    Arg     arg[20];
    char * officialName;
    
    if(motifInitialized == 1)
    	return 0;
    
    if (loadMotif() != 0)
    	return -1;
    
    /* Save the arguments in case displayMessage() is called in the main launcher. */ 
    if (saveArgv == 0)
    {
    	saveArgc = *pArgc;
    	saveArgv =  argv;
    }  

    officialName = getOfficialName();
    if (officialName != NULL)
    	setenv("RESOURCE_NAME", getOfficialName(), 1);
    
    /* Create the top level shell that will not be used other than
       to initialize the application. 
     */
#ifdef AIX
    topWindow = motif.eclipseXtInitialize(NULL, officialName, NULL, 0, pArgc, argv);
#else
	topWindow = motif.XtInitialize(NULL, officialName, NULL, 0, pArgc, argv);
#endif
	appContext = motif.XtWidgetToApplicationContext(topWindow);
	motif.XtSetLanguageProc (appContext, NULL, NULL);
	motif_XtSetArg( arg[ 0 ], XmNmappedWhenManaged, False );
    motif.XtSetValues( topWindow, arg, 1 );
    motif.XtRealizeWidget( topWindow );
    motifInitialized = 1;
    return 0;
}

/* Centre the shell on the screen. */
void centreShell( Widget widget, Widget expose )
{
    XtAppContext context;
    XEvent       event;
    Arg          arg[20];
    int          nArgs;
    Position     x, y;
    Dimension    width, height;
    Screen*      screen;
    int          waiting;
    short        screenWidth, screenHeight;
    
#ifndef NO_XINERAMA_EXTENSIONS
    Display*            display;
    int                 monitorCount;
    XineramaScreenInfo* info;
#endif
	
    /* Realize the shell to calculate its width/height. */
    motif.XtRealizeWidget( widget );

    /* Get the desired dimensions of the shell. */
    nArgs = 0;
    motif_XtSetArg( arg[ nArgs ], XmNwidth,  &width  ); nArgs++;
    motif_XtSetArg( arg[ nArgs ], XmNheight, &height ); nArgs++;
    motif_XtSetArg( arg[ nArgs ], XmNscreen, &screen ); nArgs++;
    motif.XtGetValues( widget, arg, nArgs );

	screenWidth = screen->width;
	screenHeight = screen->height;
#ifndef NO_XINERAMA_EXTENSIONS
	display = motif_XtDisplay( widget );
	if (motif.XineramaIsActive != 0 && motif.XineramaIsActive( display )) {
		info = motif.XineramaQueryScreens( display, &monitorCount );
		if (info != 0) {
			if (monitorCount > 1) {
				screenWidth = info->width;
				screenHeight = info->height;
			}
			motif.XFree (info);
		}
	}
#endif
	
    /* Calculate the X and Y position for the shell. */
    x = (screenWidth - width) / 2;
    y = (screenHeight - height) / 2;

    /* Set the new shell position and display it. */
    nArgs = 0;
    motif_XtSetArg( arg[ nArgs ], XmNx, x ); nArgs++;
    motif_XtSetArg( arg[ nArgs ], XmNy, y ); nArgs++;
    motif.XtSetValues( widget, arg, nArgs );
    motif_XtMapWidget( widget );

    /* Wait for an expose event on the desired widget. This wait loop is required when
     * the startVM command fails and the message box is created before the splash
     * window is displayed. Without this wait, the message box sometimes appears
     * under the splash window and the user cannot see it.
     */
    context = motif.XtWidgetToApplicationContext( widget );
    waiting = True;
    while (waiting) 
    {
        motif.XtAppNextEvent( context, &event );
        if (event.xany.type == Expose && event.xany.window == motif_XtWindow( expose )) 
        {
            waiting = False;
        }
        motif.XtDispatchEvent( &event );
    }
    motif.XFlush( motif_XtDisplay( widget ) );
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

