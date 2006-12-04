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
#include <stdlib.h>
#include <Xm/XmAll.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/IntrinsicP.h>
#include <X11/Intrinsic.h>
#include <X11/Shell.h>

#ifndef NO_XINERAMA_EXTENSIONS
#include <X11/extensions/Xinerama.h>
#endif

#define ECLIPSE_ICON  401

char   dirSeparator  = '/';
char   pathSeparator = ':';

void centreShell( Widget widget, Widget expose );
void initWindowSystem( int* pArgc, _TCHAR* argv[], int showSplash );

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
    
    /* If there is no associated display, just print the error and return. */
    displayName = getenv("DISPLAY");
    if (displayName == NULL || strlen(displayName) == 0) 
    {
    	printf( "%s: %s\n", title, message );
    	return;
    }
   
    /* If Xt has not been initialized yet, do it now. */
    if (topWindow == 0) 
    {
		initWindowSystem( &saveArgc, saveArgv, 1 );
    }
    
	msg = XmStringGenerate( message, NULL, XmCHARSET_TEXT, NULL );

    /* Output a simple message box. */
    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNdialogType, XmDIALOG_MESSAGE ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNtitle, title ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmessageString, msg ); nArgs++;
    msgBox = XmCreateMessageDialog( topWindow, officialName, arg, nArgs );
    XtUnmanageChild( XmMessageBoxGetChild( msgBox, XmDIALOG_CANCEL_BUTTON ) );
    XtUnmanageChild( XmMessageBoxGetChild( msgBox, XmDIALOG_HELP_BUTTON ) );
    XtManageChild( msgBox );
    centreShell( msgBox, msgBox );
    if (msg != 0) XmStringFree (msg);

    /* Wait for the OK button to be pressed. */
    while (XtIsRealized( msgBox ) && XtIsManaged( msgBox ))
    {
        XtAppNextEvent( appContext, &event );
        XtDispatchEvent( &event );
    }
    XtDestroyWidget( msgBox );
}

/* Initialize Window System
 *
 * Initialize the Xt and Xlib.
 */
void initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
    Arg     arg[20];
    
    if(motifInitialized == 1)
    	return;
    /* Save the arguments in case displayMessage() is called in the main launcher. */ 
    if (saveArgv == 0)
    {
    	saveArgc = *pArgc;
    	saveArgv =  argv;
    }  
    
    /* Create the top level shell that will not be used other than
       to initialize the application. */
	XtSetLanguageProc (NULL, NULL, NULL);
    topWindow = XtAppInitialize( &appContext, officialName, NULL, 0,
                                 pArgc, argv, NULL, NULL, 0 ); 
    XtSetArg( arg[ 0 ], XmNmappedWhenManaged, False );
    XtSetValues( topWindow, arg, 1 );
    XtRealizeWidget( topWindow );
    motifInitialized = 1;
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
    XtRealizeWidget( widget );

    /* Get the desired dimensions of the shell. */
    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNwidth,  &width  ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNheight, &height ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNscreen, &screen ); nArgs++;
    XtGetValues( widget, arg, nArgs );

	screenWidth = screen->width;
	screenHeight = screen->height;
#ifndef NO_XINERAMA_EXTENSIONS
	display = XtDisplay( widget );
	if (XineramaIsActive( display )) {
		info = XineramaQueryScreens( display, &monitorCount );
		if (info != 0) {
			if (monitorCount > 1) {
				screenWidth = info->width;
				screenHeight = info->height;
			}
			XFree (info);
		}
	}
#endif
	
    /* Calculate the X and Y position for the shell. */
    x = (screenWidth - width) / 2;
    y = (screenHeight - height) / 2;

    /* Set the new shell position and display it. */
    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNx, x ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNy, y ); nArgs++;
    XtSetValues( widget, arg, nArgs );
    XtMapWidget( widget );

    /* Wait for an expose event on the desired widget. This wait loop is required when
     * the startVM command fails and the message box is created before the splash
     * window is displayed. Without this wait, the message box sometimes appears
     * under the splash window and the user cannot see it.
     */
    context = XtWidgetToApplicationContext( widget );
    waiting = True;
    while (waiting) 
    {
        XtAppNextEvent( context, &event );
        if (event.xany.type == Expose && event.xany.window == XtWindow( expose )) 
        {
            waiting = False;
        }
        XtDispatchEvent( &event );
    }
    XFlush( XtDisplay( widget ) );
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

