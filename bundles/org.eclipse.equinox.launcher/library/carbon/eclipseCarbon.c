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

#include <CoreServices/CoreServices.h>
#include <Carbon/Carbon.h>
#include <mach-o/dyld.h>

#include "NgCommon.h"
#include "NgImageData.h"
#include "NgWinBMPFileFormat.h"

#define startupJarName "startup.jar"
#define LAUNCHER "-launcher"
#define SPLASH_LAUNCHER "/Resources/Splash.app/Contents/"

#define DEBUG 0

char *findCommand(char *command);

static PixMapHandle loadBMPImage(const char *image);

/* Global Variables */
char*  consoleVM     = "java";
char*  defaultVM     = "java";
char*  shippedVMDir  = "jre/bin/";

/* Define the window system arguments for the various Java VMs. */
static char*  argVM_JAVA[] = { "-XstartOnFirstThread", NULL };

static int jvmPid, jvmExitCode = 0;

static WindowRef window;
static ControlRef progress = NULL, pane = NULL;
static RGBColor foreground = {0xFFFF, 0xFFFF, 0xFFFF};
static CFStringRef string = NULL;
static Rect messageRect = {0, 0, 0, 0}, progressRect = {0, 0, 0, 0};
static int value = 0, maximum = 0;
static int splashHandle = 0;

int main() {
}
static void detectExitTimerProc(EventLoopTimerRef inTimer, void *inUserData) {
	int exitCode = 0;
	if (waitpid(jvmPid, &exitCode, WNOHANG) != 0) {
		jvmExitCode = exitCode;
		QuitApplicationEventLoop();
	}
}

static void invalidateWindow () {
	Rect rect;
	RgnHandle rgn;
	ControlRef root;
	rgn = NewRgn();
	GetRootControl(window, &root);
	GetControlBounds(root, &rect);
	SetRectRgn(rgn, rect.left, rect.top, rect.right, rect.bottom);
	InvalWindowRgn (window, rgn);
	DisposeRgn(rgn);
}

static OSStatus drawProc (EventHandlerCallRef eventHandlerCallRef, EventRef eventRef, PixMap * pixmap) {
	Rect rect;
	int result = CallNextEventHandler(eventHandlerCallRef, eventRef);
	GrafPtr port = GetWindowPort(window);
	SetPort(port);
	GetControlBounds(pane, &rect);
	CopyBits((BitMap*)pixmap, GetPortBitMapForCopyBits(port), &rect, &rect, srcCopy, NULL);
	if (string != NULL) {
		RGBForeColor(&foreground);
		DrawThemeTextBox(string, kThemeSmallSystemFont, kThemeStateActive, 1, &messageRect, teFlushLeft, NULL);
	}
	return result;
}

/* Show the Splash Window
 *
 * Create the splash window, load the bitmap and display the splash window.
 */
int showSplash( const _TCHAR* featureImage )
{
	Rect wRect;
	int w, h, deviceWidth, deviceHeight;
	PixMap *pm;
	PixMapHandle pixmap;
	EventTypeSpec draw = {kEventClassControl, 	kEventControlDraw};

	/*debug("featureImage: %s\n", featureImage);*/

	/*init();*/

	pixmap= loadBMPImage(featureImage);
	/* If the splash image data could not be loaded, return an error. */
	if (pixmap == NULL)
		return ENOENT;
		
	pm= *pixmap;
	w= pm->bounds.right;
	h= pm->bounds.bottom;

	GetAvailableWindowPositioningBounds(GetMainDevice(), &wRect);

	deviceWidth= wRect.right - wRect.left;
	deviceHeight= wRect.bottom - wRect.top;

	wRect.left+= (deviceWidth-w)/2;
	wRect.top+= (deviceHeight-h)/3;
	wRect.right= wRect.left + w;
	wRect.bottom= wRect.top + h;

	CreateNewWindow(kModalWindowClass, kWindowStandardHandlerAttribute, &wRect, &window);
	if (window != NULL) {
		ControlRef root = NULL;
		CreateRootControl(window, &root);
		GetRootControl(window, &root);
		SetRect(&wRect, 0, 0, w, h);
		CreateUserPaneControl(window, &wRect, 0, &pane);
		EmbedControl(pane, root);
		CreateProgressBarControl(window, &progressRect, value, 0, maximum, 0, &progress);
		EmbedControl(progress, pane);
		InstallEventHandler(GetControlEventTarget(pane), (EventHandlerUPP)drawProc, 1, &draw, pm, NULL);
		/*readInput();
		InstallEventLoopTimer (GetCurrentEventLoop (), 50 / 1000.0, 50 / 1000.0, NewEventLoopTimerUPP(timerProc), NULL, NULL);*/
		ShowWindow(window);
		splashHandle = (int)window;
/*		RunApplicationEventLoop();
		DisposeWindow(window);
		if (string != NULL) CFRelease(string);
		string = NULL;
		progress = pane = NULL;*/
	}

	return 0;
}

int getSplashHandle() {
	return splashHandle;
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

/**
 * loadBMPImage
 * Create a QuickDraw PixMap representing the given BMP file.
 *
 * bmpPathname: absolute path and name to the bmp file
 *
 * returned value: the PixMapHandle newly created if successful. 0 otherwise.
 */
static PixMapHandle loadBMPImage (const char *bmpPathname) { 
	ng_stream_t in;
	ng_bitmap_image_t image;
	ng_err_t err= ERR_OK;
	PixMapHandle pixmap;
	PixMap *pm;

	NgInit();

	if (NgStreamInit(&in, (char*) bmpPathname) != ERR_OK) {
		NgError(ERR_NG, "Error can't open BMP file");
		return 0;
	}

	NgBitmapImageInit(&image);
	err= NgBmpDecoderReadImage (&in, &image);
	NgStreamClose(&in);

	if (err != ERR_OK) {
		NgBitmapImageFree(&image);
		return 0;
	}

	UBYTE4 srcDepth= NgBitmapImageBitCount(&image);
	if (srcDepth != 24) {	/* We only support image depth of 24 bits */
		NgBitmapImageFree(&image);
		NgError (ERR_NG, "Error unsupported depth - only support 24 bit");
		return 0;
	}

	pixmap= NewPixMap();	
	if (pixmap == 0) {
		NgBitmapImageFree(&image);
		NgError(ERR_NG, "Error XCreatePixmap failed");
		return 0;
	}

	pm= *pixmap;

	int width= (int)NgBitmapImageWidth(&image);
	int height= (int)NgBitmapImageHeight(&image);
	int rowBytes= width * 4;
	
	pm->bounds.right= width;
	pm->bounds.bottom= height;
	pm->rowBytes= rowBytes + 0x8000; 
	pm->baseAddr= NewPtr(rowBytes * height);
	pm->pixelType= RGBDirect;
	pm->pixelSize= 32;
	pm->cmpCount= 3;
	pm->cmpSize= 8;

	/* 24 bit source to direct screen destination */
	NgBitmapImageBlitDirectToDirect(NgBitmapImageImageData(&image), NgBitmapImageBytesPerRow(&image), width, height,
		(UBYTE1*)pm->baseAddr, pm->pixelSize, rowBytes, NgIsMSB(),
			0xff0000, 0x00ff00, 0x0000ff);

	NgBitmapImageFree(&image);

	return pixmap;
}

char * findVMLibrary( char* command ) {
	return "/System/Library/Frameworks/JavaVM.framework/Versions/Current/JavaVM";
}
