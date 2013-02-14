/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *******************************************************************************/
 
/* Photon specific logic for displaying the splash screen. */

#include "eclipseOS.h"
#include "eclipseUtil.h"
#include <Pt.h>

#define PX_IMAGE_MODULES
#define PX_BMP_SUPPORT

#include <photon/PxImage.h>

#include <sys/types.h>
#include <sys/wait.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <locale.h>
#include <libgen.h>

/* Global Variables */
char   dirSeparator  = '/';
char   pathSeparator = ':';
#ifndef J9VM
char*  defaultVM     = "java";
char*  shippedVMDir  = "jre/bin/";
#else
char*  defaultVM     = "j9";
char*  shippedVMDir  = "ive/bin/";
#endif

/* Define the window system arguments for the various Java VMs. */
static char*  argVM_JAVA[] = { NULL };
static char*  argVM_J9[]   = { "-jit", "-ms:32", "-mso:256", NULL };


/* Define local variables for the main window. */
static PtWidget_t*  topWindow = NULL;

/* Define local variables for running the JVM and detecting its exit. */
static pid_t   jvmProcess = 0;
static int     jvmExitCode;

/* Local functions */
static void       bringDownSplashWindow( int );
static void       centreWindow( PtWidget_t *widget, PtWidget_t *label);
static int        splashTimeout( PtWidget_t* widget, void* data, PtCallbackInfo_t* id );

/* Display a Message */
void displayMessage( char* title, char* message )
{
    if (topWindow == 0) 
    {
		initWindowSystem( NULL, NULL, 0 );
    }
    
 	PtNotice( NULL,NULL, title, NULL, message, NULL, NULL, NULL, Pt_CENTER | Pt_MODAL );
}


/* Initialize Window System
 *
 * Initialize Photon.
 */
void initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
    PtArg_t arg[5];
    int     nArgs;
   
    /* Create a top level window with no decorations. */
    setlocale(LC_ALL, "");
    PtInit( NULL );
    nArgs = 0;
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_WINDOW_RENDER_FLAGS, 0, ~0 ); 
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_WINDOW_MANAGED_FLAGS, Ph_WM_TASKBAR | Ph_WM_CLOSE, ~0 ); 
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_WINDOW_STATE, Ph_WM_STATE_ISFRONT, ~0 ); 
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_WINDOW_TITLE, getOfficialName(), ~0 ); 
    topWindow = PtCreateWidget( PtWindow, Pt_NO_PARENT, nArgs, arg );
}


/* Show the Splash Window
 *
 * Create the splash window, load the bitmap and display the splash window.
 *
 */
int showSplash( char* timeoutString, char* featureImage )
{
	int          timeout = 0;
    PtWidget_t*  label;
    PtArg_t      arg[10];
    PhImage_t*   image = NULL;
    int          nArgs;
    int          depth;
	PgDisplaySettings_t settings;
	PgVideoModeInfo_t   mode_info;
    
	/* Determine the splash timeout value (in seconds). */
	if (timeoutString != NULL && strlen( timeoutString ) > 0)
	{
	    sscanf( timeoutString, "%d", &timeout );
	}
	
    /* Install a signal handler to catch SIGUSR2 (which will shut down the window). */
    signal( SIGUSR2, bringDownSplashWindow );
  
	/* Load the splash image from the feature directory. */
    PgGetVideoMode( &settings );
    PgGetVideoModeInfo( settings.mode, &mode_info );
    depth = mode_info.bits_per_pixel;
    if (featureImage != NULL)
    	image = PxLoadImage( featureImage, NULL );

    /* If the splash image could not be found, return an error. */
    if (image == NULL)
    	return ENOENT;

    /* Create a label widget (only child of top window) with the image. */
    nArgs = 0;
	image->flags |= Ph_RELEASE_IMAGE_ALL;
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_LABEL_TYPE, Pt_IMAGE, 0 );
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_LABEL_IMAGE, image, 0 );
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_TEXT_STRING, officialName, 0 );
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_MARGIN_WIDTH, 0, 0 );
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_MARGIN_HEIGHT, 0, 0 );
    label = PtCreateWidget( PtLabel, topWindow, nArgs, arg );
    
    /* Free the image */
    free( image );

    /* Centre the splash window and display it. */
    centreWindow( topWindow, label );
	
	/* If a timeout for the splash window was given */
	if (timeout != 0)
	{
        PtAddEventHandler( topWindow, Ph_EV_TIMER, splashTimeout, NULL );
        PtTimerArm( topWindow, (timeout * 1000) );
	}

    /* Process messages until the splash window is closed or process is terminated. */
 	while (PtWidgetIsRealized( topWindow ))
 	{
  	    PtProcessEvent();
    }

    /* Destroy the splash window. */
    PtDestroyWidget( topWindow );
    topWindow = 0;

    return 0;
}


/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) 
{
    return (isJ9VM( vm ) ? argVM_J9 : argVM_JAVA);
}

void fixEnvForJ9( char* vm ) {
    if (isJ9VM( vm )) {
        char   *ldpath;
        char   newpath[PATH_MAX+1];

        ldpath = getenv( "LD_LIBRARY_PATH" );

        /* Always dup the string so we can free later */
        if( ldpath != NULL )
            ldpath = strdup( ldpath );
        else
            ldpath = strdup( "" );

        /* Get the j9 binary location */ 
        strncpy( newpath, vm, PATH_MAX );
        dirname( newpath );

		/* Add j9 binary location to LD_LIBRARY_PATH */
        ldpath = realloc( ldpath, strlen( ldpath ) + strlen( newpath ) + 2 );
        if( ldpath != NULL )
        {
            strcat( ldpath, ":" );
            strcat( ldpath, newpath );
        }

        setenv( "LD_LIBRARY_PATH", ldpath, 1 );

        free( ldpath );
    }
}

/* Start the Java VM 
 *
 * This method is called to start the Java virtual machine and to wait until it
 * terminates. The function returns the exit code from the JVM.
 */
int startJavaVM( char* args[] ) 
{
    int    exitCode;
    
    fixEnvForJ9 (args [0]);
	
	/* Create a child process for the JVM. */	
	jvmProcess = fork();
	if (jvmProcess == 0) 
	{
		/* Child process ... start the JVM */
		execv( args[0], args );
		
		/* The JVM would not start ... return error code to parent process. */
		jvmExitCode = errno;
        exit( jvmExitCode );
	}
	
	/* If the JVM is still running, wait for it to terminate. */
	if (jvmProcess != 0)
	{
		wait( &exitCode );
		jvmExitCode = ((exitCode & 0x00ff) == 0 ? (exitCode >> 8) : exitCode); /* see wait(2) */
	}
	
	/* Return the exit code from the JVM. */
	return jvmExitCode;
}

/*------ Local functions -----*/


/* Catch a signal that indicates the splash window is to be brought down. */
static void bringDownSplashWindow( int sig )
{
    if (topWindow != 0) 
    {
        PtUnrealizeWidget( topWindow );
    }
}


/* Centre the top window on the screen. */

static void centreWindow( PtWidget_t* window, PtWidget_t* expose )
{
    PtArg_t   arg[2];
    int   nArgs;
    PhPoint_t pos;
	PhArea_t area;
    PhRect_t rect;
    int width, height;

    /* Realize the top window to calculate its width/height. */
    PtExtentWidgetFamily( window );

    /* Get the desired dimensions of the window. */
    PtWidgetArea( window, &area );

    /* Calculate the X and Y position for the window. */
    PhWindowQueryVisible( Ph_QUERY_WORKSPACE, 0, PhInputGroup(0), &rect );
    width = rect.lr.x - rect.ul.x + 1;
    height = rect.lr.y - rect.ul.y + 1;
    pos.x = rect.ul.x + (width  - area.size.w) / 2;
    pos.y = rect.ul.y + (height - area.size.h) / 2;

    /* Set the new shell position and display it. */
    nArgs = 0;
    PtSetArg( &arg[ nArgs++ ], Pt_ARG_POS, &pos, 0 );
    PtSetResources( window, nArgs, arg );
    PtRealizeWidget( window );
}


/* Splash Timeout */
static int splashTimeout( PtWidget_t* widget, void* data, PtCallbackInfo_t* info )
{
    bringDownSplashWindow( 0 );
   	return 1;
}
