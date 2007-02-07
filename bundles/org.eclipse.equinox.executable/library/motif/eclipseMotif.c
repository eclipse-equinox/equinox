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
#include "eclipseCommon.h"
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

/* Global Variables */
char*  defaultVM     = "java";
char*  vmLibrary 	 = "libjvm.so";
char*  shippedVMDir  = "jre/bin/";

/* Define the special arguments for the various Java VMs. */
static char*  argVM_JAVA[]        = { NULL };
#if AIX
static char*  argVM_JAVA_AIX131[] = { "-Xquickstart", NULL };
#endif
static char*  argVM_J9[]          = { "-jit", "-mca:1024", "-mco:1024", "-mn:256", "-mo:4096", 
									  "-moi:16384", "-mx:262144", "-ms:16", "-mr:16", NULL };
								 	   
/* Define local variables for the main window. */
extern XtAppContext appContext;
extern Widget       topWindow;

static pid_t   jvmProcess = 0;
static int     jvmExitCode;

/* Define local variables for handling the splash window and its image. */
static Widget shellHandle = 0;

extern void   centreShell( Widget widget, Widget expose );

#ifdef NETSCAPE_FIX
static void   fixEnvForNetscape();
#endif /* NETSCAPE_FIX */

/* Show the Splash Window
 *
 * Create the splash window, load the pixmap and display the splash window.
 */
int showSplash( const char* featureImage )
{
	int argc [] = {0};
	int x, y;
	unsigned int width, height, depth, border;
	ArgList args;
	unsigned int nArgs;
	Pixmap  splashPixmap = 0;
	Window  root;
	Display *xDisplay;
	Screen* screen;
	Widget scrolledHandle, drawingHandle, image;
	
	initWindowSystem(&initialArgc, initialArgv, 1);
	
	xDisplay = XtOpenDisplay(appContext, NULL, NULL, NULL, 0, 0, argc, 0);
    screen = XDefaultScreenOfDisplay( xDisplay );
    if (featureImage != NULL)
    {
    	splashPixmap = loadBMPImage(xDisplay, screen, (char*)featureImage);
    }
        /* If the splash image could not be found, return an error. */
    if (splashPixmap == 0)
    	return ENOENT;
    
    XGetGeometry (xDisplay, splashPixmap, &root, &x, &y, &width, &height, &border, &depth);
    
    /* make sure we never pass more than 20 args */
    args = malloc(10 * sizeof(Arg));
    
    nArgs = 0;
    /* Note that XtSetArg is a macro, and the 1st argument will be evaluated twice
     * so increment nArgs on its own */
	XtSetArg(args[nArgs], XmNmwmDecorations, 0);	nArgs++;
    XtSetArg(args[nArgs], XmNtitle, getOfficialName());	nArgs++;
    XtSetArg(args[nArgs], XmNwidth, width);			nArgs++;
    XtSetArg(args[nArgs], XmNheight, height);		nArgs++;
	shellHandle = XtAppCreateShell(getOfficialName(), "", applicationShellWidgetClass, xDisplay, args, nArgs);								   
	
	nArgs = 0;
	XtSetArg(args[nArgs++], XmNancestorSensitive, 1);
	scrolledHandle = XmCreateMainWindow(shellHandle, NULL, args, nArgs);
	if(scrolledHandle == 0)
		return -1;
	XtManageChild(scrolledHandle);
	
	nArgs = 0;
	XtSetArg(args[nArgs], XmNancestorSensitive, 1);			nArgs++;
    XtSetArg(args[nArgs], XmNborderWidth, 0);				nArgs++;
    /*XtSetArg(args[nArgs], XmNbackground, 0xFF00FF);			nArgs++; */
    XtSetArg(args[nArgs], XmNmarginWidth, 0);				nArgs++;
    XtSetArg(args[nArgs], XmNmarginHeight, 0);				nArgs++;
    XtSetArg(args[nArgs], XmNresizePolicy, XmRESIZE_NONE);	nArgs++;
    XtSetArg(args[nArgs], XmNtraversalOn, 1);				nArgs++;
	drawingHandle = XmCreateDrawingArea(scrolledHandle, NULL, args, nArgs);
	if(drawingHandle == 0)
		return -1;
	XtManageChild(drawingHandle);
	
	nArgs = 0;
	XtSetArg(args[nArgs], XmNlabelType, XmPIXMAP);		nArgs++;
    XtSetArg(args[nArgs], XmNlabelPixmap, splashPixmap);nArgs++;
    XtSetArg(args[nArgs], XmNwidth, width);				nArgs++;
    XtSetArg(args[nArgs], XmNheight, height);			nArgs++;
    XtSetArg(args[nArgs], XmNmarginWidth, 0);			nArgs++;
    XtSetArg(args[nArgs], XmNmarginHeight, 0);			nArgs++;
    image = XmCreateLabelGadget ( drawingHandle, "", args, nArgs );
    XtManageChild( image );
	
	XtRealizeWidget(shellHandle);
	XtSetMappedWhenManaged(shellHandle, 1);

	if(XtIsTopLevelShell(shellHandle))
		XtMapWidget(shellHandle);
	else
		XtPopup(shellHandle, XtGrabNone);
		
	/* Centre the splash screen and display it. */
    centreShell( shellHandle, drawingHandle );
    dispatchMessages();
    
    free(args);
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


long getSplashHandle() {
	return (long)shellHandle;
}

void dispatchMessages() {
	XtInputMask mask;
	if (appContext != NULL) {
		/* Process any outstanding messages */
	 	while ((mask = XtAppPending(appContext)) != 0) {
	 		XtAppProcessEvent(appContext, mask);
	 	}
	}
}

void takeDownSplash()
{
    if (shellHandle != 0) 
    {
        XtUnrealizeWidget( shellHandle );
        XFlush( XtDisplay( shellHandle ) );
    }
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

int launchJavaVM( char* args[] ) 
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
