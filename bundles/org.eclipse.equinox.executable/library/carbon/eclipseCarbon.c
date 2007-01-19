/*
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    IBM Corporation - initial API and implementation
 * 	  Andre Weinand (OTI Labs)
 */
 
/* MacOS X Carbon specific logic for displaying the splash screen. */

#include "eclipseOS.h"
#include "eclipseCommon.h"

#include <unistd.h>
#include <CoreServices/CoreServices.h>
#include <Carbon/Carbon.h>
#include <mach-o/dyld.h>

#define startupJarName "startup.jar"
#define LAUNCHER "-launcher"
#define SPLASH_LAUNCHER "/Resources/Splash.app/Contents/"

#define DEBUG 0

char *findCommand(char *command);

/* Global Variables */
char*  defaultVM     = "java";
char*  shippedVMDir  = "jre/bin/";

/* Define the window system arguments for the various Java VMs. */
static char*  argVM_JAVA[] = { "-XstartOnFirstThread", NULL };

static WindowRef window;
static ControlRef pane = NULL;
static CGImageRef image = NULL;
static long splashHandle = 0;

int main() {
	return -1;
}

static OSStatus drawProc (EventHandlerCallRef eventHandlerCallRef, EventRef eventRef, HIViewRef viewRef) {
	ControlRef control;
	CGContextRef context;
	
	GetEventParameter(eventRef, kEventParamDirectObject, typeControlRef, NULL, 4, NULL, &control);
	GetEventParameter(eventRef, kEventParamCGContextRef, typeCGContextRef, NULL, 4, NULL, &context);
	
	HIRect rect;
	HIViewGetBounds(viewRef, &rect);
	HIViewDrawCGImage(context, &rect, image);
	return eventNotHandledErr;
}

/* Show the Splash Window
 *
 * Create the splash window, load the bitmap and display the splash window.
 */
int showSplash( const _TCHAR* featureImage )
{
	Rect wRect;
	int w, h, deviceWidth, deviceHeight;
	int attributes;
	EventTypeSpec draw = {kEventClassControl, kEventControlDraw};
	ControlRef root;
	
	/*debug("featureImage: %s\n", featureImage);*/

	/*init();*/

	CFStringRef imageString = CFStringCreateWithCString(kCFAllocatorDefault, featureImage, kCFStringEncodingASCII);
	if(imageString != NULL) {
		CFURLRef url = CFURLCreateWithFileSystemPath(kCFAllocatorDefault, imageString, kCFURLPOSIXPathStyle, false);
		if(url != NULL) {
			CGImageSourceRef imageSource = CGImageSourceCreateWithURL(url, NULL);
			if(imageSource != NULL) {
				image = CGImageSourceCreateImageAtIndex(imageSource, 0, NULL);
			}
			CFRelease(url);
		}
	}
	CFRelease(imageString);
	/* If the splash image data could not be loaded, return an error. */
	if (image == NULL)
		return ENOENT;
		
	w = CGImageGetWidth(image);
	h = CGImageGetHeight(image);

	GetAvailableWindowPositioningBounds(GetMainDevice(), &wRect);

	deviceWidth= wRect.right - wRect.left;
	deviceHeight= wRect.bottom - wRect.top;

	wRect.left+= (deviceWidth-w)/2;
	wRect.top+= (deviceHeight-h)/3;
	wRect.right= wRect.left + w;
	wRect.bottom= wRect.top + h;
	
	attributes = kWindowStandardHandlerAttribute | kWindowCompositingAttribute;
	attributes &= GetAvailableWindowAttributes(kSheetWindowClass);
	CreateNewWindow(kSheetWindowClass, attributes, &wRect, &window);
	if (window != NULL) {
		GetRootControl(window, &root);
		wRect.left = wRect.top = 0;	
		wRect.right = w;
		wRect.bottom = h;
		CreateUserPaneControl(window, &wRect, kControlSupportsEmbedding | kControlSupportsFocus | kControlGetsFocusOnClick, &pane);
		HIViewAddSubview(root, pane);	

		InstallEventHandler(GetControlEventTarget(pane), (EventHandlerUPP)drawProc, 1, &draw, pane, NULL);
		ShowWindow(window);
		splashHandle = (long)window;
		dispatchMessages();
	}

	return 0;
}

long getSplashHandle() {
	return splashHandle;
}

void takeDownSplash() {
	if( splashHandle != 0) {
		HideWindow(window);
		DisposeWindow(window);
		dispatchMessages();
		splashHandle = 0;
	}
}	
void dispatchMessages() {
	EventRef event;
	EventTargetRef target;
	
	target = GetEventDispatcherTarget();
	while( ReceiveNextEvent(0, NULL, kEventDurationNoWait, true, &event) == noErr ) {
		SendEventToEventTarget(event, target);
		ReleaseEvent(event);
	}
}	

/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) 
{
	char** result;

	/* Use the default arguments for a standard Java VM */
	result = argVM_JAVA;

	return result;
}

char * findVMLibrary( char* command ) {
	char *start, *end;
	char *version;
	int length;
	
	/*check first to see if command already points to the library */
	start = strrchr( command, dirSeparator ) + 1;
	if (strcmp(start, "JavaVM") == 0) {
		return command;
	}
		
	/* select a version to use based on the command */	
	start = strstr(command, "/Versions/");
	if (start != NULL){
		start += 10;
		end = strchr( start, dirSeparator);
		if (end != NULL) {
			length = end - start;
			version = malloc(length);
			strncpy(version, start, length);
			version[length] = 0;
			
			setenv("JAVA_JVM_VERSION", version, 1);
		} 
	}
	return "/System/Library/Frameworks/JavaVM.framework/Versions/Current/JavaVM";
}


void restartLauncher( char* program, char* args[] ) 
{
	pid_t pid= fork();
	if (pid == 0) {
		/* Child process ... start the JVM */
		execv(program, args);

		/* The JVM would not start ... return error code to parent process. */
		_exit(errno);
	}
}

int launchJavaVM( _TCHAR* args[] )
{
	/*for now always do JNI on Mac, should not come in here */
	return -1;
}