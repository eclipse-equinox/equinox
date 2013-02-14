/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
#include "eclipseMotif.h"
#include "eclipseOS.h"
#include "eclipseUtil.h"
#include "NgImage.h"

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
void   fixEnvForNetscape();
#endif /* NETSCAPE_FIX */

void takeDownSplashCB( Widget shell, XtPointer app_data, XtPointer widget_data ) {
  shellHandle = NULL;
}

/* Show the Splash Window
 *
 * Create the splash window, load the pixmap and display the splash window.
 */
int showSplash( const char* featureImage )
{
	int x, y;
	unsigned int width, height, depth, border;
	ArgList args;
	unsigned int nArgs;
	Pixmap  splashPixmap = 0;
	Window  root;
	Display *xDisplay;
	Screen* screen;
	Widget scrolledHandle, drawingHandle, image;
	
	if (shellHandle != 0)
		return 0; /* already showing splash */
	
	if (initialArgv == NULL) 
		initialArgc = 0;

	if (initWindowSystem(&initialArgc, initialArgv, 1) != 0) {
		return -1;
	}

	xDisplay = motif_XtDisplay(topWindow);
    screen = motif.XDefaultScreenOfDisplay( xDisplay );
    if (featureImage != NULL)
    {
    	splashPixmap = loadBMPImage(xDisplay, screen, (char*)featureImage);
    }
        /* If the splash image could not be found, return an error. */
    if (splashPixmap == 0)
    	return ENOENT;
    
    motif.XGetGeometry (xDisplay, splashPixmap, &root, &x, &y, &width, &height, &border, &depth);
    
    /* make sure we never pass more than 20 args */
    args = malloc(10 * sizeof(Arg));
    
    nArgs = 0;
    /* Note that XtSetArg is a macro, and the 1st argument will be evaluated twice
     * so increment nArgs on its own */
	motif_XtSetArg(args[nArgs], XmNmwmDecorations, 0);	nArgs++;
    motif_XtSetArg(args[nArgs], XmNtitle, getOfficialName());	nArgs++;
    motif_XtSetArg(args[nArgs], XmNwidth, width);			nArgs++;
    motif_XtSetArg(args[nArgs], XmNheight, height);		nArgs++;
	shellHandle = motif.XtAppCreateShell(getOfficialName(), "", *motif.applicationShellWidgetClass, xDisplay, args, nArgs);								   
	motif.XtAddCallback(shellHandle, XmNdestroyCallback, (XtCallbackProc) takeDownSplashCB, NULL);
	
	nArgs = 0;
	motif_XtSetArg(args[nArgs++], XmNancestorSensitive, 1);
	scrolledHandle = motif.XmCreateMainWindow(shellHandle, NULL, args, nArgs);
	if(scrolledHandle == 0)
		return -1;
	motif.XtManageChild(scrolledHandle);
	
	nArgs = 0;
	motif_XtSetArg(args[nArgs], XmNancestorSensitive, 1);			nArgs++;
    motif_XtSetArg(args[nArgs], XmNborderWidth, 0);					nArgs++;
    /*motif_XtSetArg(args[nArgs], XmNbackground, 0xFF00FF);			nArgs++; */
    motif_XtSetArg(args[nArgs], XmNmarginWidth, 0);					nArgs++;
    motif_XtSetArg(args[nArgs], XmNmarginHeight, 0);				nArgs++;
    motif_XtSetArg(args[nArgs], XmNresizePolicy, XmRESIZE_NONE);	nArgs++;
    motif_XtSetArg(args[nArgs], XmNtraversalOn, 1);					nArgs++;
	drawingHandle = motif.XmCreateDrawingArea(scrolledHandle, NULL, args, nArgs);
	if(drawingHandle == 0)
		return -1;
	motif.XtManageChild(drawingHandle);
	
	nArgs = 0;
	motif_XtSetArg(args[nArgs], XmNlabelType, XmPIXMAP);		nArgs++;
    motif_XtSetArg(args[nArgs], XmNlabelPixmap, splashPixmap);	nArgs++;
    motif_XtSetArg(args[nArgs], XmNwidth, width);				nArgs++;
    motif_XtSetArg(args[nArgs], XmNheight, height);				nArgs++;
    motif_XtSetArg(args[nArgs], XmNmarginWidth, 0);				nArgs++;
    motif_XtSetArg(args[nArgs], XmNmarginHeight, 0);			nArgs++;
    image = motif.XmCreateLabelGadget ( drawingHandle, "", args, nArgs );
    motif.XtManageChild( image );
	
	motif.XtRealizeWidget(shellHandle);
	motif.XtSetMappedWhenManaged(shellHandle, 1);

	if(motif_XtIsTopLevelShell(shellHandle))
		motif_XtMapWidget(shellHandle);
	else
		motif.XtPopup(shellHandle, XtGrabNone);
		
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


jlong getSplashHandle() {
	return (jlong)shellHandle;
}

void dispatchMessages() {
	XtInputMask mask;
	if (appContext != NULL && motif.XtAppPending != 0) {
		/* Process any outstanding messages */
	 	while ((mask = motif.XtAppPending(appContext)) != 0) {
	 		motif.XtAppProcessEvent(appContext, mask);
	 	}
	}
}

void takeDownSplash()
{
    if (shellHandle != 0) 
    {
        motif.XtDestroyWidget( shellHandle );
        /*XFlush( XtDisplay( shellHandle ) );*/
        shellHandle = NULL;
    }
}

#ifdef NETSCAPE_FIX
extern char* findCommand( char*);
static const char*  XFILESEARCHPATH = "XFILESEARCHPATH";

void   fixEnvForNetscape()
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

JavaResults* launchJavaVM( char* args[] ) 
{
	JavaResults* jvmResults = NULL;
    int    exitCode;

#ifdef NETSCAPE_FIX
	fixEnvForNetscape();
#endif /* NETSCAPE_FIX */
#ifdef MOZILLA_FIX
	fixEnvForMozilla();
#endif /* MOZILLA_FIX */

#ifdef LINUX
	{
		/* put the root of eclipse on the LD_LIBRARY_PATH */
		char * ldPath = (char*)getenv(_T_ECLIPSE("LD_LIBRARY_PATH"));
		if (ldPath == NULL)
			ldPath = _T_ECLIPSE("");
		char * root = getProgramDir();
		if (root != NULL) {
			char * newPath = malloc((strlen(root) + strlen(ldPath) + 2) * sizeof(char));
			sprintf(newPath, "%s%c%s", root, pathSeparator, ldPath);
			setenv("LD_LIBRARY_PATH", newPath, 1);
			free(newPath);
		}
	}
#endif
	
	/* Create a child process for the JVM. */	
	jvmProcess = fork();
	if (jvmProcess == 0) 
	{
		/* Child process ... start the JVM */
		execv( args[0], args );
		
		/* The JVM would not start ... return error code to parent process. */
		/* TODO, how to distinguish this as a launch problem to the other process? */
		jvmExitCode = errno;
        exit( jvmExitCode );
	}
	
	jvmResults = malloc(sizeof(JavaResults));
	memset(jvmResults, 0, sizeof(JavaResults));
	
	/* If the JVM is still running, wait for it to terminate. */
	if (jvmProcess != 0)
	{
		waitpid(jvmProcess, &exitCode, 0);
  		/* TODO, this should really be a runResult if we could distinguish the launch problem above */
		jvmResults->launchResult = ((exitCode & 0x00ff) == 0 ? (exitCode >> 8) : exitCode); /* see wait(2) */
	}
	
	/* Return the exit code from the JVM. */
	return jvmResults;
}

int reuseWorkbench(_TCHAR** filePath, int timeout) {
	/* not yet implemented on motif */
	return -1;
}
