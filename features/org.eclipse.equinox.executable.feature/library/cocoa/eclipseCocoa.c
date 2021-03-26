/*
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 * 	  Andre Weinand (OTI Labs)
 *    David Green - OpenJDK bsd port integration
 *    Rapicorp, Inc - Default the configuration to Application Support (bug 461725)
 *    Mikael Barbero - Rename *Carbon* files to *Cocoa* (bug 383545)
 *    Christian Georgi (SAP SE) - Fix VM path for new file layout (bug 469766)
 */

/* MacOS X Cocoa specific logic for displaying the splash screen. */

#include "eclipseOS.h"
#include "eclipseCommon.h"
#include "eclipseJNI.h"
#include "eclipseUtil.h"

#include <sys/xattr.h>
 #include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <CoreServices/CoreServices.h>
#include <Cocoa/Cocoa.h>
#include <mach-o/dyld.h>

#define startupJarName "startup.jar"
#define LAUNCHER "-launcher"
#define SPLASH_LAUNCHER "/Resources/Splash.app/Contents/"

#define DEBUG 0

static _TCHAR* noForkingMsg = _T_ECLIPSE("Internal Error, forking the jvm is not supported on MacOS.\n");
static const _TCHAR* INSTALL_UUID = _T_ECLIPSE("eclipse.uuid");

char *findCommand(char *command);

/* Global Variables */
char*  defaultVM     = "java";
char*  vmLibrary	 = "JavaVM";
char*  shippedVMDir  = "../../jre/Contents/Home/bin/"; // relative to launcher
int isModularJVM = 0;

static void adjustLibraryPath(char * vmLibrary);
static char * findLib(char * command);

#ifdef i386
#define JAVA_ARCH "i386"
#define JAVA_HOME_ARCH "i386"
#elif defined(__amd64__) || defined(__x86_64__)
#define JAVA_ARCH "amd64"
#define JAVA_HOME_ARCH "x86_64"
#else
#define JAVA_ARCH DEFAULT_OS_ARCH
#define JAVA_HOME_ARCH DEFAULT_OS_ARCH
#endif

#define LIB_PATH_VAR _T_ECLIPSE("LD_LIBRARY_PATH")
#define DYLD_FALLBACK_VAR _T_ECLIPSE("DYLD_FALLBACK_LIBRARY_PATH")

#define MAX_LOCATION_LENGTH 40 /* none of the jvmLocations strings should be longer than this */
#define MAX_JVMLIB_LENGTH   15 /* none of the jvmLibs strings should be longer than this */
static const char* jvmLocations[] = {
	"../lib/" JAVA_ARCH "/client",
	"../lib/" JAVA_ARCH "/server",
	"../lib/client",
	"../lib/server",
	"../jre/lib/" JAVA_ARCH "/client",
	"../jre/lib/" JAVA_ARCH "/server",
	"../jre/lib/client",
	"../jre/lib/server",
	NULL
};
static const char* jvmLibs[] = { "libjvm.dylib", "libjvm.jnilib", "libjvm.so", NULL };

/* Define the window system arguments for the various Java VMs. */
static char*  argVM_JAVA[] = { "-XstartOnFirstThread", NULL };

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

+ (int)show: (NSString *) featureImage {
	if (window != NULL)
		return 0; /*already showing */
	if (featureImage == NULL)
		return ENOENT;

	int result = ENOENT;
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	[NSApplication sharedApplication];
	NSImage* image = [[NSImage alloc] initByReferencingFile: featureImage];
	[featureImage release];
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

+ (void)shutdown {
	if (window != 0) {
		NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
		[window close];
		window = nil;
		[pool release];
	}
}

+ (void)dispatch {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	NSEvent* event;
	NSApplication* application = [NSApplication sharedApplication];
	while ((event = [application nextEventMatchingMask: NSAnyEventMask untilDate: nil inMode: NSDefaultRunLoopMode dequeue: TRUE]) != nil) {
		[application sendEvent: event];
	}
	[pool release];
}

@end

@interface AppleEventDelegate : NSObject
- (void)handleOpenDocuments:(NSAppleEventDescriptor *)event withReplyEvent: (NSAppleEventDescriptor *)replyEvent;
- (void)handleGetURL:(NSAppleEventDescriptor *)event withReplyEvent: (NSAppleEventDescriptor *)replyEvent;
@end
@implementation AppleEventDelegate
	NSTimer *timerOpenDocuments;
	NSMutableArray *files;
	NSTimer *timerOpenUrls;
	NSMutableArray *urls;

- (void)handleOpenDocuments:(NSAppleEventDescriptor *)event withReplyEvent: (NSAppleEventDescriptor *)replyEvent {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    int count = [event numberOfItems];
    int index = 1;

 	if (!files) {
		files = [NSMutableArray arrayWithCapacity:count];
		[files retain];
	}

	for (index = 1; index<=count; index++) {
		CFURLRef url = NULL;
		NSAppleEventDescriptor *desc = [event descriptorAtIndex:index], *coerceDesc;
		if (!desc) continue;
		if ((coerceDesc = [desc coerceToDescriptorType: typeFSRef]) != NULL) {
			url = CFURLCreateFromFSRef(kCFAllocatorDefault, [[coerceDesc data] bytes]);
		} else if ((coerceDesc = [desc coerceToDescriptorType: typeFileURL]) != NULL) {
			NSData *data = [coerceDesc data];
			url = CFURLCreateWithBytes(kCFAllocatorDefault, [data bytes], [data length], kCFStringEncodingUTF8, NULL);
		}
		if (url) {
			NSString *pathName = (NSString *)CFURLCopyFileSystemPath(url, kCFURLPOSIXPathStyle);
			[files addObject:pathName];
			[pathName release];
			CFRelease(url);
		}
	}

	if (!timerOpenDocuments) {
		timerOpenDocuments = [NSTimer scheduledTimerWithTimeInterval: 1.0
												 target: self
											   selector: @selector(handleOpenDocumentsTimer:)
											   userInfo: nil
												repeats: YES];
	}
	[pool release];
}

- (void) handleOpenDocumentsTimer: (NSTimer *) timer {
	NSObject *delegate = [[NSApplication sharedApplication] delegate];
	if (delegate != NULL && [delegate respondsToSelector: @selector(application:openFiles:)]) {
		[delegate performSelector:@selector(application:openFiles:)	withObject:[NSApplication sharedApplication] withObject:files];
		[files release];
		files = NULL;
		[timerOpenDocuments invalidate];
	}
}

- (void)handleGetURL:(NSAppleEventDescriptor *)event withReplyEvent:(NSAppleEventDescriptor *)replyEvent {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

	NSString *url = [[event paramDescriptorForKeyword:keyDirectObject] stringValue];

	NSObject *delegate = [[NSApplication sharedApplication] delegate];
	if (delegate != NULL && [delegate respondsToSelector: @selector(application:openUrls:)]) {
		[delegate performSelector:@selector(application:openUrls:) withObject:[NSApplication sharedApplication] withObject:[NSArray arrayWithObject:url]];
	} else {
		if (!urls) {
			urls = [NSMutableArray arrayWithCapacity:1];
			[urls retain];
		}

		[urls addObject:url];

		if (!timerOpenUrls) {
			timerOpenUrls = [NSTimer scheduledTimerWithTimeInterval: 1.0
									 target: self
									 selector: @selector(handleOpenUrlsTimer:)
									 userInfo: nil
									 repeats: YES];
		}
	}

	[pool release];
}

- (void) handleOpenUrlsTimer: (NSTimer *) timer {
	NSObject *delegate = [[NSApplication sharedApplication] delegate];
	if (delegate != NULL && [delegate respondsToSelector: @selector(application:openUrls:)]) {
		[delegate performSelector:@selector(application:openUrls:)	withObject:[NSApplication sharedApplication] withObject:urls];
		[urls release];
		urls = NULL;
		[timerOpenUrls invalidate];
		timerOpenUrls = NULL;
	}
}
@end

int main() {
	return -1;
}

void installAppleEventHandler();

int reuseWorkbench(_TCHAR** filePath, int timeout) {
	installAppleEventHandler();
	return 0;
}

/* Show the Splash Window
 *
 * Create the splash window, load the bitmap and display the splash window.
 */
int showSplash( const _TCHAR* featureImage )
{
	int result = 0;
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	NSString *str = [[NSString stringWithUTF8String: featureImage] retain];
	if ([NSThread isMainThread]) {
		result = [KeyWindow show: str];
	} else {
		[KeyWindow performSelectorOnMainThread: @selector(show:) withObject: str waitUntilDone: 0];
	}
	[pool release];
	return result;
}

void takeDownSplash() {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	if ([NSThread isMainThread]) {
		[KeyWindow shutdown];
	} else {
		[KeyWindow performSelectorOnMainThread: @selector(shutdown) withObject: nil waitUntilDone: 0];
	}
	[pool release];
}

void dispatchMessages() {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	if ([NSThread isMainThread]) {
		[KeyWindow dispatch];
	} else {
		[KeyWindow performSelectorOnMainThread: @selector(dispatch) withObject: nil waitUntilDone: 0];
	}
	[pool release];
}

void installAppleEventHandler() {
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	AppleEventDelegate *appleEventDelegate = [[AppleEventDelegate alloc] init];
	[NSApplication sharedApplication];
	NSAppleEventManager *manager = [NSAppleEventManager sharedAppleEventManager];
	[manager setEventHandler:appleEventDelegate
				 andSelector:@selector(handleOpenDocuments:withReplyEvent:)
			   forEventClass:kCoreEventClass
				  andEventID:kAEOpenDocuments];
	[manager setEventHandler:appleEventDelegate
				 andSelector:@selector(handleGetURL:withReplyEvent:)
			   forEventClass:kInternetEventClass
				  andEventID:kAEGetURL];
//	[appleEventDelegate release];
	[pool release];
}

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

/* set isModularJVM to 1 if the JVM version is >= 9, 0 otherwise */
void checkJavaVersion(char* command) {
	FILE *fp;
	char buffer[4096];
	char *version = NULL, *firstChar;
    int numChars = 0;
	sprintf(buffer,"%s -version 2>&1", command);
	fp = popen(buffer, "r");
	if (fp == NULL) {
		return;
	}
	while (fgets(buffer, sizeof(buffer)-1, fp) != NULL) {
		if (!version) {
			firstChar = (char *) (strchr(buffer, '"') + 1);
			if (firstChar != NULL)
				numChars = (int)  (strrchr(buffer, '"') - firstChar);

			/* Allocate a buffer and copy the version string into it. */
			if (numChars > 0)
			{
				version = malloc( numChars + 1 );
				strncpy(version, firstChar, numChars);
				version[numChars] = '\0';
			}
		}
		if (version != NULL) {
			char *str = version;
			/* According to the new Java version-string scheme, the first element is
			 * the major version number, details at http://openjdk.java.net/jeps/223 */
			char *majorVersion = strtok(str, ".-");
			if (majorVersion != NULL && (strtol(majorVersion, NULL, 10) >= 9)) {
				isModularJVM = 1;
			}
			free(version);
		}
		break;
	}
	pclose(fp);
	return;
}

char * getJavaHome() {
	FILE *fp;
	char path[4096];
	char *result, *start;
	sprintf(path, "/usr/libexec/java_home -a %s", JAVA_HOME_ARCH);
	fp = popen(path, "r");
	if (fp == NULL) {
		return NULL;
	}
	path[0] = 0;
	while (fgets(path, sizeof(path)-1, fp) != NULL) {
	}
	if(path[0]==0) {
		return NULL;
	}
	result = path;
	start = strchr(result, '\n');
	if (start) {
		start[0] = 0;
	}
	sprintf(path, "%s/bin/java", result);
	pclose(fp);
	return strdup(path);
}

char * findVMLibrary( char* command ) {
	char *start, *end;
	char *version, *result, *cmd;
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
	cmd = command;
	if ((strstr(cmd, "/JavaVM.framework/") != NULL && (strstr(cmd, "/Current/") != NULL)) || strstr(cmd, "/A/") != NULL || strstr(cmd, "/usr/bin/java") != NULL ) {
	        //On Mac we have universal library at /usr/bin/java. But actual java is some where else. So if the path received is default path we need look for java home
		cmd = getJavaHome();
		if (cmd == NULL) {
			return NULL;
		}
	}
	// This is necessary to initialize isModularJVM
	checkJavaVersion(cmd);
	result = JAVA_FRAMEWORK;
	if (strstr(cmd, "/JavaVM.framework/") == NULL) {
		char * lib = findLib(cmd);
		if (lib != NULL) {
			// This does not seem to be necessary to load the Mac JVM library
			if (0) adjustLibraryPath(lib);
			result = lib;
		}
	}
	if (cmd != command) free(cmd);
	return result;
}

static char * findLib(char * command) {
	int i, q;
	int pathLength;
	struct stat stats;
	char * path; /* path to resulting jvm shared library */
	char * location; /* points to begining of jvmLocations section of path */

	if (command != NULL) {
		/*check first to see if command already points to the library */
		if (isVMLibrary(command)) {
			if (stat(command, &stats) == 0 && (stats.st_mode & S_IFREG) != 0) { /* found it */
				return strdup(command);
			}
			return NULL;
		}

		location = strrchr(command, dirSeparator) + 1;
		pathLength = location - command;
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + MAX_JVMLIB_LENGTH	+ 1) * sizeof(char));
		strncpy(path, command, pathLength);
		location = &path[pathLength];

		/*
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		for (q = 0; jvmLibs[q] != NULL; ++q) {
			const char *jvmLib = jvmLibs[q];
			i = -1;
			while (jvmLocations[++i] != NULL) {
				sprintf(location, "%s%c%s", jvmLocations[i], dirSeparator, jvmLib);
				/*fprintf(stderr,"checking path: %s\n",path);*/
				if (stat(path, &stats) == 0 && (stats.st_mode & S_IFREG) != 0)
				{ /* found it */
					return path;
				}
			}
		}
	}
	return NULL;
}

/* adjust the LD_LIBRARY_PATH for the vmLibrary */
static void adjustLibraryPath(char * vmLibrary) {
	char * c;
	char * ldPath, *dylibPath;
	char * newPath;
	int i;
	int numPaths = 0;
	int length = 0;
	int needAdjust = 0, needDylibAdjust = 0;

	char ** paths = getVMLibrarySearchPath(vmLibrary);

	ldPath = (char*) getenv(LIB_PATH_VAR);
	if (!ldPath) {
		ldPath = _T_ECLIPSE("");
		needAdjust = 1;
	} else {
		needAdjust = !containsPaths(ldPath, paths);
	}

	dylibPath = (char*) getenv(DYLD_FALLBACK_VAR);
	if (!dylibPath) {
		dylibPath = _T_ECLIPSE("");
		needDylibAdjust = 1;
	} else {
		needDylibAdjust = !containsPaths(dylibPath, paths);
	}

	if (!needAdjust && !needDylibAdjust) {
		for (i = 0; paths[i] != NULL; i++)
			free(paths[i]);
		free(paths);
		return;
	}

	c = concatStrings(paths);

	/* set the value for LD_LIBRARY_PATH */
	length = strlen(ldPath);
	newPath = malloc((_tcslen(c) + length + 1) * sizeof(_TCHAR));
	_stprintf(newPath, _T_ECLIPSE("%s%s"), c, ldPath);
	setenv(LIB_PATH_VAR, newPath, 1);
	free(newPath);

	/* set the value for DYLD_FALLBACK_LIBRARY_PATH */
	length = strlen(dylibPath);
	newPath =  malloc((_tcslen(c) + length + 1) * sizeof(_TCHAR));
	_stprintf(newPath, _T_ECLIPSE("%s%s"), c, dylibPath);
	setenv(DYLD_FALLBACK_VAR, newPath, 1);
	free(newPath);
	free(c);

	for (i = 0; i < numPaths; i++)
		free(paths[i]);
	free(paths);

	/* now we must restart for this to take affect*/
	restartLauncher(initialArgv[0], initialArgv);
}

void restartLauncher(char* program, char* args[]) {
	pid_t pid= fork();
	if (pid == 0) {
		/* Child process ... start the JVM */
		execv(program != NULL ? program : args[0], args);

		/* The JVM would not start ... return error code to parent process. */
		_exit(errno);
	} else {
		exit(0);
	}
}

JavaResults* launchJavaVM( _TCHAR* args[] )
{
	/*
	 * On macOS, JNI is always used, so this point is only reached, if no usable JVM was found.
	 * Request a JDK install: The 'script' command makes stdin and stdout look like a tty,
	 * so it looks for /usr/libexec/java_home that this is for a command line tool and a dialog
	 * with a link to the JDK download page is shown.
	 */
	execl("/usr/bin/script", "/usr/bin/script", "/dev/null", "/usr/libexec/java_home", "--request", NULL);

	/* not reached. */
	JavaResults * results = malloc(sizeof(JavaResults));
	results->launchResult = -1;
	results->runResult = 0;
	results->errorMessage = _tcsdup(noForkingMsg);
	return results;
}

JavaResults* startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile )
{
	return startJavaJNI(libPath, vmArgs, progArgs, jarFile);
}

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

int isModularVM( _TCHAR * javaVM, _TCHAR * jniLib ) {
	return isModularJVM;
}

NSString* getApplicationSupport() {
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
	NSString *documentsDirectory = [paths objectAtIndex:0];
	return documentsDirectory;

}

NSString* getCFBundleIdentifier() {
	CFBundleRef mainBundle= CFBundleGetMainBundle();
	return (NSString*) CFBundleGetIdentifier(mainBundle);
}

const char* getUUID() {
	const char * installPath = [[[NSBundle mainBundle] resourcePath] fileSystemRepresentation];
	int bufferLength = getxattr(installPath, INSTALL_UUID,  NULL, 0, 0, 0);
	if (bufferLength != -1) {
		char *buffer = malloc(bufferLength + 1);
		buffer[bufferLength] = '\0';
		getxattr(installPath, INSTALL_UUID, buffer, bufferLength, 0, 0);
		return buffer;
	}

	NSString * timestamp = [NSString stringWithFormat:@"%f",[[NSDate date] timeIntervalSince1970] * 1000];
	const char* timestampAsChar = [timestamp UTF8String];
    setxattr(installPath, INSTALL_UUID, timestampAsChar, strlen(timestampAsChar), 0, 0);
    return timestampAsChar;
}

_TCHAR* getFolderForApplicationData() {
	NSString* bundleId = getCFBundleIdentifier();
	NSString* appSupport = getApplicationSupport();
	return [[NSString stringWithFormat:@"%@/%@_%s", appSupport, bundleId, getUUID()] UTF8String];
}
