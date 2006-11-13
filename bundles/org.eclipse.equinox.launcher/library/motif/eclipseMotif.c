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
 *******************************************************************************/
 
 
/* UNIX/Motif specific logic for displaying the splash screen. */

#include "eclipseMozilla.h"
#include "eclipseOS.h"
#include "eclipseUtil.h"
#include "NgImage.h"

#include <Xm/XmAll.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/IntrinsicP.h>
#include <X11/Intrinsic.h>
#include <X11/Shell.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#ifdef SOLARIS
#include <sys/filio.h>
#endif
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <locale.h>
#include <stdlib.h>

#ifndef NO_XINERAMA_EXTENSIONS
#include <X11/extensions/Xinerama.h>
#endif

/* Global Variables */
char   dirSeparator  = '/';
char   pathSeparator = ':';
char*  consoleVM     = "java";
char*  defaultVM     = "java";
char*  shippedVMDir  = "jre/bin/";

/* Define the special arguments for the various Java VMs. */
static char*  argVM_JAVA[]        = { NULL };
#if AIX
static char*  argVM_JAVA_AIX131[] = { "-Xquickstart", NULL };
#endif
static char*  argVM_J9[]          = { "-jit", "-mca:1024", "-mco:1024", "-mn:256", "-mo:4096", 
									  "-moi:16384", "-mx:262144", "-ms:16", "-mr:16", NULL };


/* Define local variables for the main window. */
static XtAppContext appContext = 0;
static Widget       topWindow  = 0;
static int          saveArgc   = 0;
static char**       saveArgv   = 0;

/* Define local variables for running the JVM and detecting its exit. */
static pid_t   jvmProcess = 0;
static int     jvmExitCode;

/* Define local variables for handling the splash window and its image. */
static Widget splashShell = 0;
static Widget label = NULL;
static Widget progress = NULL;
static XColor foreground = {0, 0, 0, 0, 0, 0};
static XRectangle progressRect = {0, 0, 0, 0}, messageRect = {0, 0, 0, 0};
static int value = 0, maximum = 100;

/* Local functions */
static void   bringDownSplashWindow( int );
static void   centreShell( Widget widget, Widget expose );
static void   splashTimeout( XtPointer data, XtIntervalId* id );
#ifdef NETSCAPE_FIX
static void   fixEnvForNetscape();
#endif /* NETSCAPE_FIX */

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
    
    /* Save the arguments in case displayMessage() is called in the main launcher. */ 
    if (saveArgv == 0)
    {
    	saveArgc = *pArgc;
    	saveArgv =  argv;
    }  
    
    /* If the splash screen is going to be displayed by this process */
    if (showSplash)
    {
	    /* Create the top level shell that will not be used other than
	       to initialize the application. */
  		XtSetLanguageProc (NULL, NULL, NULL);
	    topWindow = XtAppInitialize( &appContext, officialName, NULL, 0,
	                                 pArgc, argv, NULL, NULL, 0 ); 
	    XtSetArg( arg[ 0 ], XmNmappedWhenManaged, False );
	    XtSetValues( topWindow, arg, 1 );
	    XtRealizeWidget( topWindow );
	}
}


static void readRect(char *str, XRectangle *rect) {
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

static void readColor(char *str, XColor *color) {
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
					Arg argList[1];
					XtSetArg(argList[0], XmNmaximum, maximum);
					XtSetValues (progress, argList, 1);
				}
			} else if (strcmp(line, "value") == 0) {
				value = atoi(str);
				if (progress) {
					Arg argList[1];
					XtSetArg(argList[0], XmNvalue, value);
					XtSetValues (progress, argList, 1);
				}
			} else if (strcmp(line, "progressRect") == 0) {
				readRect(str, &progressRect);
				if (progress) {
					XtConfigureWidget (progress, progressRect.x, progressRect.y, progressRect.width, progressRect.height, 0);
				}
			} else if (strcmp(line, "messageRect") == 0) {
				readRect(str, &messageRect);
				if (label) {
					XtConfigureWidget (label, messageRect.x, messageRect.y, messageRect.width, messageRect.height, 0);
				}
			} else if (strcmp(line, "foreground") == 0) {
				Arg argList[1];
				Display *xDisplay = XtDisplay(topWindow);
				readColor(str, &foreground);
				XAllocColor(xDisplay, XDefaultColormap(xDisplay, XDefaultScreen(xDisplay)), &foreground);
				XtSetArg(argList[0], XmNforeground, foreground.pixel);
				if (label) XtSetValues (label, argList, 1);
			} else if (strcmp(line, "message") == 0) {
				if (label) {
					Arg argList[2];
					XmString xmString = XmStringGenerate(str, NULL, XmCHARSET_TEXT, NULL);
					XtSetArg(argList[0], XmNlabelType, XmSTRING);
					XtSetArg(argList[1], XmNlabelString, xmString);
					XtSetValues (label, argList, 2);
					XmStringFree (xmString);
				}
			}
			
		}
		if (end != NULL) line = end + 1;
		else line = NULL;
	}
	free(buffer);
}

static void timerProc( XtPointer data, XtIntervalId* id ) {
	readInput();
	XtAppAddTimeOut( appContext, 50, timerProc, 0 );
}

/* Show the Splash Window
 *
 * Create the splash window, load the pixmap and display the splash window.
 */
int showSplash( char* timeoutString, char* featureImage )
{
	int     timeout = 0;
	Pixmap  splashPixmap = 0;
    Widget  image, drawingArea;
    Display *xDisplay;
    Window  root;
    Screen* screen;
    Arg     arg[20];
    int     nArgs;
    unsigned int width, height, depth, border;
    int x, y;
    XEvent  event;
    XSetWindowAttributes attributes;
    
	/* Determine the splash timeout value (in seconds). */
	if (timeoutString != NULL && strlen( timeoutString ) > 0)
	{
	    sscanf( timeoutString, "%d", &timeout );
	}
	
    /* Install a signal handler to catch SIGUSR2 (which will shut down the window). */
	/* Note: On Linux when the splash window process is started by the 1.3.0 IBM JVM, 
			 the USR1 signal does not get sent to our signal handler. Hence, the splash
			 window will stay displayed if the JVM exits during initialization. However,
			 USR2 appears to work.
	*/
    signal( SIGUSR2, bringDownSplashWindow );
      
    /* Load the feature specific splash image if defined. */
   	xDisplay = XtDisplay( topWindow );
    screen = XDefaultScreenOfDisplay( xDisplay );
    if (featureImage != NULL)
    {
    	splashPixmap = loadBMPImage(xDisplay, screen, featureImage);
    }
    
    /* If the splash image could not be found, return an error. */
    if (splashPixmap == 0)
    	return ENOENT;

 	XGetGeometry (xDisplay, splashPixmap, &root, &x, &y, &width, &height, &border, &depth);

    /* Create an application shell with no decorations.
     * Do not use an overrideShell because it is not known to the
     * window manager. As a result, when the error message box is
     * displayed on top of the splash window (overrideShell) button
     * events in the message box are sent to the splash window instead.
     * On Linux, this brings the splash window on top of the message
     * box which can no longer be accessed or terminated.
     */
    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNborderWidth, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmappedWhenManaged, False ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmwmDecorations, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmwmFunctions, MWM_FUNC_MOVE ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNwidth, width ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNheight, height ); nArgs++;
    splashShell = XtCreatePopupShell( officialName, transientShellWidgetClass, topWindow, arg, nArgs );

    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNborderWidth, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmarginWidth, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmarginHeight, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNresizePolicy, XmRESIZE_NONE ); nArgs++;
    drawingArea = XmCreateDrawingArea (splashShell, NULL, arg, nArgs );	
    XtManageChild( drawingArea );

    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNslidingMode, XmTHERMOMETER ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNeditable, False ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNshadowThickness, 1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNorientation,  XmHORIZONTAL ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNsliderVisual, XmFOREGROUND_COLOR); nArgs++;
    XtSetArg( arg[ nArgs ], XmNhighlightThickness, 0); nArgs++;    
    XtSetArg( arg[ nArgs ], XmNborderWidth, 0); nArgs++;
    XtSetArg( arg[ nArgs ], XmNtraversalOn, 0); nArgs++;
    XtSetArg( arg[ nArgs ], XmNx, -1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNy, -1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNwidth, 1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNheight, 1 ); nArgs++;
    progress = XmCreateScale ( drawingArea, "", arg, nArgs );	
    XtManageChild( progress );

    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNlabelType, XmPIXMAP ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNlabelPixmap, splashPixmap ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNwidth, width ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNheight, height ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmarginHeight, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmarginWidth,  0 ); nArgs++;
    image = XmCreateLabelGadget ( drawingArea, "", arg, nArgs );
    XtManageChild( image );

    nArgs = 0;
    XtSetArg( arg[ nArgs ], XmNlabelType, XmSTRING ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmarginHeight, 0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNmarginWidth,  0 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNx, -1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNy, -1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNwidth, 1 ); nArgs++;
    XtSetArg( arg[ nArgs ], XmNheight, 1 ); nArgs++;
    label = XmCreateLabelGadget ( drawingArea, "", arg, nArgs );
    XtManageChild( label );

    /* Set the background pixmap to None to avoid a gray flash when the image appears. */
    XtRealizeWidget( splashShell );
    attributes.background_pixmap = None;
    XChangeWindowAttributes( XtDisplay( drawingArea ), XtWindow( drawingArea ), CWBackPixmap, &attributes );

    readInput();
    XtAppAddTimeOut( appContext, 50, timerProc, 0 );

    /* Centre the splash screen and display it. */
    centreShell( splashShell, drawingArea );

	/* If a timeout for the splash window was given */
	if (timeout != 0)
	{
		/* Add a timeout (in milliseconds) to bring down the splash screen. */
        XtAppAddTimeOut( appContext, (timeout * 1000), splashTimeout, 0 );
	}

    /* Process messages until the splash window is closed or process is terminated. */
 	while (XtIsRealized( splashShell ))
 	{
        XtAppNextEvent( appContext, &event );
        XtDispatchEvent( &event );
    }
    
    /* Clean up. */
    XFreePixmap( XtDisplay(splashShell), splashPixmap );
    return 0;
}


/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) 
{
    char** result;
   
#ifdef AIX
    char*  version;
#endif

    if (isJ9VM( vm )) 
        return argVM_J9;
    
    /* Use the default arguments for a standard Java VM */
    result = argVM_JAVA;
    
#ifdef AIX
	/* Determine whether Java version is 1.3.1 or later */
	version = getVMVersion( vm );
	if (version != NULL) 
	{
	    if (versionCmp(version, "1.3.1") >= 0)
	        result = argVM_JAVA_AIX131;
	    free(version);
	}
#endif

    return result;
}


/* Start the Java VM 
 *
 * This method is called to start the Java virtual machine and to wait until it
 * terminates. The function returns the exit code from the JVM.
 */
int startJavaVM( char* args[] ) 
{
    int    exitCode;

#ifdef NETSCAPE_FIX
	fixEnvForNetscape();
#endif /* NETSCAPE_FIX */
#ifdef MOZILLA_FIX
	fixEnvForMozilla();
#endif /* MOZILLA_FIX */

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

/*------ local functions -----*/

/* Catch a signal that indicates the splash window is to be brought down. */
static void   bringDownSplashWindow( int sig )
{
    if (splashShell != 0) 
    {
        XtUnrealizeWidget( splashShell );
        XFlush( XtDisplay( splashShell ) );
    }
}

/* Splash Window Timeout */
static void splashTimeout( XtPointer data, XtIntervalId* id )
{
	bringDownSplashWindow(0);
}

/* Centre the shell on the screen. */
static void centreShell( Widget widget, Widget expose )
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
#ifdef NETSCAPE_FIX
extern char* findCommand( char*);
static const char*  XFILESEARCHPATH = "XFILESEARCHPATH";

static void   fixEnvForNetscape()
{
	char*  netscapePath     = NULL;
	char*  netscapeResource = NULL;
	char*  ch;
	char*  envValue;
	struct stat   stats;

	/* If netscape appears to be installed */
	netscapePath = findCommand("netscape");
	if (netscapePath != NULL)
	{
		/* Look for the resource file Netscape.ad in the same directory as "netscape". */
		netscapeResource = malloc( strlen(netscapePath) + 50 );
		strcpy( netscapeResource, netscapePath );
		ch = strrchr( netscapeResource, (int) dirSeparator );
		ch =(ch == NULL ? netscapeResource : (ch+1));
		strcpy( ch, "Netscape.ad" );
		
		/* If it does not exist there, try "/opt/netscape/Netscape.ad". */
        if (stat( netscapeResource, &stats ) != 0)
        {
        	strcpy( netscapeResource, "/opt/netscape/Netscape.ad" );
        }
        
        /* If the resource file exists */
        if (stat( netscapeResource, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
        {
        	/* Either define XFILESEARCHPATH or append the Netscape resource file. */
        	envValue = getenv( XFILESEARCHPATH );
        	if (envValue == NULL)
        	{
        		ch = malloc( strlen(XFILESEARCHPATH) + strlen(netscapeResource) + 5 );
        		sprintf( ch, "%s=%s", XFILESEARCHPATH, netscapeResource );
        	}
        	else
        	{
        		ch = malloc( strlen(XFILESEARCHPATH) + strlen(netscapeResource) + 
        					 strlen(envValue) + 5 );
        		sprintf( ch, "%s=%s:%s", XFILESEARCHPATH, envValue, netscapeResource );
        	}
        	putenv( ch );
        	free( ch );
        }
		
		/* Clean up. */
		free( netscapePath );
		free( netscapeResource );
	}
	
}
#endif /* NETSCAPE_FIX */
