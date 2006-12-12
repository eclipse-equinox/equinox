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

#ifdef i386
#define JAVA_ARCH "i386"
#elif defined(__ppc__)
#define JAVA_ARCH "ppc"
#elif defined(SOLARIS)
#define JAVA_ARCH "sparc"
#endif

#define MAX_LOCATION_LENGTH 20 /* none of the jvmLocations strings should be longer than this */ 
static const char* jvmLocations [] = { "j9vm",
									   "classic",
									   "../lib/" JAVA_ARCH "/client",  
									   "../lib/" JAVA_ARCH "/server",
								 	   NULL };
								 	   
/* Define local variables for the main window. */
extern XtAppContext appContext;
extern Widget       topWindow;

/* Define local variables for handling the splash window and its image. */
static Widget shellHandle = 0;

extern void   centreShell( Widget widget, Widget expose );

static void adjustLibraryPath( char * vmLibrary );
static char * findLib(char * command);

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
    XtSetArg(args[nArgs], XmNtitle, officialName);	nArgs++;
    XtSetArg(args[nArgs], XmNwidth, width);			nArgs++;
    XtSetArg(args[nArgs], XmNheight, height);		nArgs++;
	shellHandle = XtAppCreateShell(officialName, "", applicationShellWidgetClass, xDisplay, args, nArgs);								   
	
	nArgs = 0;
	XtSetArg(args[nArgs++], XmNancestorSensitive, 1);
	scrolledHandle = XmCreateMainWindow(shellHandle, NULL, args, nArgs);
	if(scrolledHandle == 0)
		return -1;
	XtManageChild(scrolledHandle);
	
	nArgs = 0;
	XtSetArg(args[nArgs], XmNancestorSensitive, 1);			nArgs++;
    XtSetArg(args[nArgs], XmNborderWidth, 0);				nArgs++;
    XtSetArg(args[nArgs], XmNbackground, 0xFF00FF);			nArgs++;
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
	/* Process any outstanding messages */
 	while ((mask = XtAppPending(appContext)) != 0) {
 		XtAppProcessEvent(appContext, mask);
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
char * findVMLibrary( char* command ) {
	char * lib = findLib(command);
	if( lib != NULL ) {
		adjustLibraryPath(lib);
	}
	return lib;
}
static char * findLib(char * command) {
	int i;
	int pathLength;	
	struct stat stats;
	char * path;				/* path to resulting jvm shared library */
	char * location;			/* points to begining of jvmLocations section of path */
	
	if (command != NULL) {
		location = strrchr( command, dirSeparator ) + 1;
		
		/*check first to see if command already points to the library */
		if (strcmp(location, vmLibrary) == 0) {
			return command;
		}
		
		pathLength = location - command;
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + strlen(vmLibrary) + 1) * sizeof(char));
		strncpy(path, command, pathLength);
		location = &path[pathLength];
		 
		/* 
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		i = -1;
		while(jvmLocations[++i] != NULL) {
			int length = strlen(jvmLocations[i]);			
			strcpy(location, jvmLocations[i]);
			location[length] = dirSeparator;
			location[length + 1] = 0;
			strcat(location, vmLibrary);
			if (stat( path, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return path;
			}
		}
	}
	return NULL;
}

/* adjust the LD_LIBRARY_PATH for the vmLibrary */
static void adjustLibraryPath( char * vmLibrary ) {
	char * buffer;
	char * path;
	char * c;
	char * vmPath;
	char * vmParent;
	char * ldPath;
	char * newPath;
	int vmPathFound = 0;
	int vmParentFound = 0;
	int ldPathLength = 0;
	
#ifdef MOZILLA_FIX
	fixEnvForMozilla();
#endif /* MOZILLA_FIX */

	/* we want the directory containing the library, and the parent directory of that */
	buffer = strdup(vmLibrary);	 
	c = strrchr(buffer, dirSeparator);
	*c = 0;
	vmPath = strdup(buffer);
	
	c = strrchr(buffer, dirSeparator);
	*c = 0;
	vmParent = strdup(buffer);
	free(buffer);
	 
	ldPath = (char*)getenv("LD_LIBRARY_PATH");
	if(!ldPath)
		ldPath = "";
	buffer = malloc((strlen(ldPath) + 2) * sizeof(char));
	strcpy(buffer, ldPath);
	strcat(buffer, ":"); 
	path = buffer;
	while( (c = strchr(path, pathSeparator)) != NULL ) {
		*c++ = 0;
		if( !vmPathFound && strcmp(path, vmPath) == 0 ) {
			vmPathFound = 1;
		} else if( !vmParentFound && strcmp(path, vmParent) == 0 ) {
			vmParentFound = 1;
		} 
		if(vmPathFound && vmParentFound)
			break;
		path = c;
	}	
	free(buffer);
	
	if( vmPathFound && vmParentFound ){
		/*found both on the LD_LIBRARY_PATH, don't need to set it */
		return;
	}
	
	/* set the value for LD_LIBRARY_PATH */
	ldPathLength = strlen(ldPath);
	/* ldPath + separator + vmPath + separator + vmParent + NULL */
	newPath = malloc((ldPathLength + 1 + strlen(vmPath) + 1 + strlen(vmParent) + 1) * sizeof(char));
	strcpy(newPath, vmPath);
	strncat(newPath, &pathSeparator, 1);
	strcat(newPath, vmParent);
	strncat(newPath, &pathSeparator, 1);
	strcat(newPath, ldPath);
	setenv( "LD_LIBRARY_PATH", newPath, 1);
	
	free(vmPath);
	free(vmParent);
	
	/* now we must restart for this to take affect */
	/* TODO what args do we restart with? */
	execv(initialArgv[0], initialArgv);
}

void restartLauncher( char* program, char* args[] ) 
{
	/* just restart in-place */
	execv(program, args);
}
