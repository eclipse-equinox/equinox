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
#include "eclipseJNI.h"

#include <unistd.h>
#include <CoreServices/CoreServices.h>
#ifdef COCOA
#include <Cocoa/Cocoa.h>
#else
#include <Carbon/Carbon.h>
#include "NgCommon.h"
#include "NgImageData.h"
#include "NgWinBMPFileFormat.h"
#endif
#include <mach-o/dyld.h>
#include <pthread.h>

#define startupJarName "startup.jar"
#define LAUNCHER "-launcher"
#define SPLASH_LAUNCHER "/Resources/Splash.app/Contents/"

#define DEBUG 0

char *findCommand(char *command);

/* Global Variables */
char*  defaultVM     = "java";
char*  vmLibrary	 = "JavaVM";
char*  shippedVMDir  = "jre/bin/";

/* Define the window system arguments for the various Java VMs. */
static char*  argVM_JAVA[] = { "-XstartOnFirstThread", NULL };

/* thread stuff */
typedef struct {
	_TCHAR * libPath;
	_TCHAR ** vmArgs;
	_TCHAR ** progArgs;
	_TCHAR * jarFile;
	int result;
} StartVMArgs;

#ifdef COCOA
static NSWindow* window = nil;
@interface KeyWindow : NSWindow { }
- (BOOL)canBecomeKeyWindow;
@end

@implementation KeyWindow
- (BOOL)canBecomeKeyWindow {
	return YES;
}

- (void)close {
	[super close];
	window = nil;
}

@end
#endif

static CFRunLoopRef loopRef = NULL;
static void * startThread(void * init); 
static void runEventLoop(CFRunLoopRef ref);
static void dummyCallback(void * info) {}

int main() {
	return -1;
}

#ifdef COCOA

/* Show the Splash Window
 *
 * Create the splash window, load the bitmap and display the splash window.
 */
int showSplash( const _TCHAR* featureImage )
{
	if (window != NULL)
		return 0; /*already showing */
	if (featureImage == NULL)
		return ENOENT;
	
	int result = ENOENT;
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	[NSApplication sharedApplication];
	NSImage* image = [[NSImage alloc] initByReferencingFile: [NSString stringWithUTF8String: featureImage]];
	if (image != NULL) {
		NSImageRep* imageRep = [image bestRepresentationForDevice: [[NSScreen mainScreen] deviceDescription]];
		NSRect rect = {{0, 0}, {[imageRep pixelsWide], [imageRep pixelsHigh]}};
		[image setSize: NSMakeSize([imageRep pixelsWide], [imageRep pixelsHigh])];
		[image autorelease];
		window = [[KeyWindow alloc] initWithContentRect: rect styleMask: NSBorderlessWindowMask backing: NSBackingStoreBuffered defer: 0];
		if (window != nil) {
			[window center];
			[window setBackgroundColor: [NSColor colorWithPatternImage: image]];
			[window makeKeyAndOrderFront: nil];
			dispatchMessages();
			result = 0;		
		}
	}
	[pool release];
	return result;
}

void takeDownSplash() {
	if (window != 0) {
		NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
		[window close];
		window = nil;
		[pool release];
	}
}	

void dispatchMessages() {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	NSEvent* event;
	NSApplication* application = [NSApplication sharedApplication];
	while ((event = [application nextEventMatchingMask: 0 untilDate: nil inMode: NSDefaultRunLoopMode dequeue: TRUE]) != nil) {
		[application sendEvent: event];
	}
	[pool release];	
}

#else
static WindowRef window;
static ControlRef pane = NULL;
static CGImageRef image = NULL;
static CGImageRef loadBMPImage(const char *image);

typedef CGImageSourceRef (*CGImageSourceCreateWithURL_FUNC) (CFURLRef, CFDictionaryRef);
typedef CGImageRef (*CGImageSourceCreateImageAtIndex_FUNC)(CGImageSourceRef, size_t, CFDictionaryRef);
static CGImageSourceCreateWithURL_FUNC createWithURL = NULL;
static CGImageSourceCreateImageAtIndex_FUNC createAtIndex = NULL;

static OSStatus drawProc (EventHandlerCallRef eventHandlerCallRef, EventRef eventRef, void * data) {
	int result = CallNextEventHandler(eventHandlerCallRef, eventRef);
	if (image) {
		ControlRef control;
		CGContextRef context;
		
		GetEventParameter(eventRef, kEventParamDirectObject, typeControlRef, NULL, 4, NULL, &control);
		GetEventParameter(eventRef, kEventParamCGContextRef, typeCGContextRef, NULL, 4, NULL, &context);
		
		HIRect rect;
		HIViewGetBounds(control, &rect);
		HIViewDrawCGImage(context, &rect, image);
	} 
	return result;
}

static OSStatus disposeProc (EventHandlerCallRef eventHandlerCallRef, EventRef eventRef, void * data) {
	window = NULL;
	return eventNotHandledErr;
}

void loadImageFns()
{
	static int initialized = 0;
	static CFBundleRef bundle = NULL;
	
	if (!initialized) {
		if (!bundle) bundle = CFBundleGetBundleWithIdentifier(CFSTR("com.apple.Carbon"));
		if (bundle) createAtIndex = (CGImageSourceCreateImageAtIndex_FUNC)CFBundleGetFunctionPointerForName(bundle, CFSTR("CGImageSourceCreateImageAtIndex"));
		if (bundle) createWithURL = (CGImageSourceCreateWithURL_FUNC)CFBundleGetFunctionPointerForName(bundle, CFSTR("CGImageSourceCreateWithURL"));
		initialized = 1;
	}
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
	EventTypeSpec dispose = {kEventClassWindow, kEventWindowDispose};
	ControlRef root;
	
	if(window != NULL)
		return 0; /*already showing */
	if (featureImage == NULL)
		return ENOENT;
	
	loadImageFns();
	if (createWithURL && createAtIndex) {
		CFStringRef imageString = CFStringCreateWithCString(kCFAllocatorDefault, featureImage, kCFStringEncodingUTF8);
		if(imageString != NULL) {
			CFURLRef url = CFURLCreateWithFileSystemPath(kCFAllocatorDefault, imageString, kCFURLPOSIXPathStyle, false);
			if(url != NULL) {
				CGImageSourceRef imageSource = createWithURL(url, NULL);
				if(imageSource != NULL) {
					image = createAtIndex(imageSource, 0, NULL);
				}
				CFRelease(url);
			}
		}
		CFRelease(imageString);		
	} else {
		image = loadBMPImage(featureImage);
	}
	
	/*If the splash image data could not be loaded, return an error.*/ 
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

		InstallEventHandler(GetControlEventTarget(pane), (EventHandlerUPP)drawProc, 1, &draw, NULL, NULL);
		InstallEventHandler(GetWindowEventTarget(window), (EventHandlerUPP)disposeProc, 1, &dispose, NULL, NULL);
		ShowWindow(window);
		dispatchMessages();
	}

	return 0;
}

void takeDownSplash() {
	if( window != 0) {
		DisposeWindow(window);
		window = NULL;
	}
	if(image){
		CGImageRelease(image);
		image = NULL;
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
#endif	

jlong getSplashHandle() {
	return (jlong)window;
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
	if (strcmp(command, JAVA_FRAMEWORK) == 0) {
		return JAVA_FRAMEWORK;
	}
		
	/* select a version to use based on the command */	
	start = strstr(command, "/Versions/");
	if (start != NULL){
		start += 10;
		end = strchr( start, dirSeparator);
		if (end != NULL && end > start) {
			length = end - start;
			version = malloc(length + 1);
			strncpy(version, start, length);
			version[length] = 0;
			
			/*only set a version if it starts with a number */
			if(strtol(version, NULL, 10) != 0 || version[0] == '0') {
				setenv("JAVA_JVM_VERSION", version, 1);
			}
			
			free(version);
		} 
	}
	return JAVA_FRAMEWORK;
}


void restartLauncher( char* program, char* args[] ) 
{
	pid_t pid= fork();
	if (pid == 0) {
		/* Child process ... start the JVM */
		execv(program != NULL ? program : args[0], args);

		/* The JVM would not start ... return error code to parent process. */
		_exit(errno);
	}
}

int launchJavaVM( _TCHAR* args[] )
{
	/*for now always do JNI on Mac, should not come in here */
	return -1;
}

int startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile )
{
	if (secondThread == 0) {
		/* Set an environment variable that tells the AWT (if started) we started the JVM on the main thread. */
		char firstThreadEnvVariable[80];
		sprintf(firstThreadEnvVariable, "JAVA_STARTED_ON_FIRST_THREAD_%d", getpid());
		setenv(firstThreadEnvVariable, "1", 1);
		return startJavaJNI(libPath, vmArgs, progArgs, jarFile);
	}

	/* else, --launcher.secondThread was specified, create a new thread and run the 
	 * vm on it.  This main thread will run the CFRunLoop 
	 */
	pthread_t thread;
	struct rlimit limit = {0, 0};
	int stackSize = 0;
	if (getrlimit(RLIMIT_STACK, &limit) == 0) {
		if (limit.rlim_cur != 0) {
			stackSize = limit.rlim_cur;
		}
	}
	
	/* initialize thread attributes */
	pthread_attr_t attributes;
	pthread_attr_init(&attributes);
	pthread_attr_setscope(&attributes, PTHREAD_SCOPE_SYSTEM);
	pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_DETACHED);
	if (stackSize != 0)
		pthread_attr_setstacksize(&attributes, stackSize);
	
	/* arguments to start the vm */
	StartVMArgs args;
	args.libPath = libPath;
	args.vmArgs = vmArgs;
	args.progArgs = progArgs;
	args.jarFile = jarFile;
	
	loopRef = CFRunLoopGetCurrent();
	
	/* create the thread */
	pthread_create( &thread, &attributes, &startThread, &args);
	pthread_attr_destroy(&attributes);
		
	runEventLoop(loopRef);
	
	return args.result;
}

void * startThread(void * init) {
	StartVMArgs *args = (StartVMArgs *) init;
	args->result = startJavaJNI(args->libPath, args->vmArgs, args->progArgs, args->jarFile);
	return NULL;
}

void runEventLoop(CFRunLoopRef ref) {
	CFRunLoopSourceContext sourceContext = { .version = 0, .info = NULL, .retain = NULL, .release = NULL,
											 .copyDescription = NULL, .equal = NULL, .hash = NULL, 
											 .schedule = NULL, .cancel = NULL, .perform = &dummyCallback };
	
	CFRunLoopSourceRef sourceRef = CFRunLoopSourceCreate(NULL, 0, &sourceContext);
	CFRunLoopAddSource(ref, sourceRef,  kCFRunLoopCommonModes);
	
	CFRunLoopRun();
	CFRelease(sourceRef);
}

#ifndef COCOA
void disposeData(void *info, void *data, size_t size) 
{
	DisposePtr(data);
}

/**
 * loadBMPImage
 * Create a QuickDraw PixMap representing the given BMP file.
 *
 * bmpPathname: absolute path and name to the bmp file
 *
 * returned value: the PixMapHandle newly created if successful. 0 otherwise.
 */
static CGImageRef loadBMPImage (const char *bmpPathname) { 
	ng_stream_t in;
	ng_bitmap_image_t image;
	ng_err_t err= ERR_OK;
	CGImageRef ref;
	UBYTE1* data = NULL;

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
	
	int width= (int)NgBitmapImageWidth(&image);
	int height= (int)NgBitmapImageHeight(&image);
	int rowBytes= width * 4;
	int alphainfo = kCGImageAlphaNoneSkipFirst | (NgIsMSB() ? 0 : kCGBitmapByteOrder32Little);
	data = (UBYTE1*)NewPtr(rowBytes * height);
	CGDataProviderRef provider = CGDataProviderCreateWithData(0, data, rowBytes * height, (CGDataProviderReleaseDataCallback)disposeData);
	
	ref = CGImageCreate(width, height, 8, 32, width * 4, CGColorSpaceCreateDeviceRGB(), alphainfo, provider, NULL, 1, 0);
	CGDataProviderRelease(provider);

	/* 24 bit source to direct screen destination */
	NgBitmapImageBlitDirectToDirect(NgBitmapImageImageData(&image), NgBitmapImageBytesPerRow(&image), width, height,
		data, 32, rowBytes, NgIsMSB(), 0xff0000, 0x00ff00, 0x0000ff);

	NgBitmapImageFree(&image);

	return ref;
}
#endif

#define DOCK_ICON_PREFIX "-Xdock:icon="
#define DOCK_NAME_PREFIX "-Xdock:name="
#define APP_ICON_PATTERN "APP_ICON_%d"
#define APP_NAME_PATTERN "APP_NAME_%d"

void processVMArgs(char **vmargs[] )
{
	int i = -1;
	int pid = 0, pidLength = 1, temp = 0;
	char * name = NULL, *icon = NULL;
	char * c;
	
	if( *vmargs == NULL)
		return;
	
	while( (*vmargs)[++i] != NULL ) {
		/*-Xdock:icon -> APP_ICON_<pid>*/
		if(_tcsncmp((*vmargs)[i], DOCK_ICON_PREFIX, _tcslen(DOCK_ICON_PREFIX)) == 0) {
			icon = (*vmargs)[i] + _tcslen(DOCK_ICON_PREFIX);
		} 
		/*-Xdock:name -> APP_NAME_<pid>*/
		else if(_tcsncmp((*vmargs)[i], DOCK_NAME_PREFIX, _tcslen(DOCK_NAME_PREFIX)) == 0) {
			name = (*vmargs)[i] + _tcslen(DOCK_NAME_PREFIX);
		}
		if (name != NULL && icon != NULL) 
			break;
	}
	
	if (name == NULL && icon == NULL)
		return;	/* don't need to do anything */
	
	temp = pid = getpid();
	/* how many digits in pid? */
	while (temp > 9) {
		pidLength++;
		temp /= 10;
	}
	
	if (name != NULL) {
		c = malloc( (_tcslen(APP_NAME_PATTERN) + pidLength + 1) * sizeof(char*));
		_stprintf( c, APP_NAME_PATTERN, pid );
		setenv(c, name, 1);
	}
	
	if (icon != NULL) {
		c = malloc( (_tcslen(icon) + _tcslen(APP_ICON_PATTERN) + pidLength + 1) * sizeof(char*));
		_stprintf( c, APP_ICON_PATTERN, pid );
		setenv(c, icon, 1);
	}
}

int isSunVM( _TCHAR * javaVM, _TCHAR * jniLib ) {
	_TCHAR *vm = (jniLib != NULL) ? jniLib : javaVM;
	return (strncmp(vm, JAVA_FRAMEWORK, strlen(JAVA_FRAMEWORK)) == 0);
}
