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

#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <ctype.h>
#include <pwd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <CoreServices/CoreServices.h>
#include <Carbon/Carbon.h>
#include <mach-o/dyld.h>

#include "NgCommon.h"
#include "NgImageData.h"
#include "NgWinBMPFileFormat.h"

#define startupJarName "startup.jar"
#define APP_PACKAGE_PATTERN ".app/Contents/MacOS/"
#define APP_PACKAGE "APP_PACKAGE"
#define JAVAROOT "JAVAROOT"
#define LAUNCHER "-launcher"
#define SPLASH_LAUNCHER "/Resources/Splash.app/Contents/"

#define DEBUG 0

char *findCommand(char *command);
char* getProgramDir();

static void debug(const char *fmt, ...);
static void dumpArgs(char *tag, int argc, char* argv[]);
static PixMapHandle loadBMPImage(const char *image);
static void init();
static char *append(char *buffer, const char *s);
static char *appendc(char *buffer, char c);
static char *expandShell(char *arg, const char *appPackage, const char *javaRoot);
static char *my_strcasestr(const char *big, const char *little);

/* Global Variables */
char   dirSeparator  = '/';
char   pathSeparator = ':';
char*  consoleVM     = "java";
char*  defaultVM     = "java";
char*  shippedVMDir  = "jre/bin/";

/* Define the window system arguments for the various Java VMs. */
static char*  argVM_JAVA[] = { "-XstartOnFirstThread", NULL };

static int fgPid, jvmPid, jvmExitCode = 0;
static FILE *fgConsoleLog;
static char *fgAppPackagePath;

static int mainEclipse = 0;
static WindowRef window;
static ControlRef progress = NULL, pane = NULL;
static RGBColor foreground = {0xFFFF, 0xFFFF, 0xFFFF};
static CFStringRef string = NULL;
static Rect messageRect = {0, 0, 0, 0}, progressRect = {0, 0, 0, 0};
static int value = 0, maximum = 0;

extern int original_main(int argc, char* argv[]);
int main( int argc, char* argv[] ) {

	SInt32 systemVersion= 0;
	if (Gestalt(gestaltSystemVersion, &systemVersion) == noErr) {
		systemVersion &= 0xffff;
		if (systemVersion < 0x1020) {
			displayMessage(officialName, "Eclipse requires Jaguar (Mac OS X >= 10.2)");
			return 0;
		}
	}

	fgConsoleLog= fopen("/dev/console", "w");
	fgPid= getpid();

	dumpArgs("start", argc, argv);
	
	if (argc > 1 && strncmp(argv[1], "-psn_", 5) == 0) {
	
		/* find path to application bundle (ignoring case) */
		char *pos= my_strcasestr(argv[0], APP_PACKAGE_PATTERN);
		if (pos != NULL) {
			int l= pos-argv[0] + 4;	// reserve space for ".app"
			fgAppPackagePath= malloc(l+1);
			strncpy(fgAppPackagePath, argv[0], l);
			fgAppPackagePath[l]= '\0';	// terminate result
		}
		
		/* Get the main bundle for the app */
		CFBundleRef mainBundle= CFBundleGetMainBundle();
		if (mainBundle != NULL) {
		
			/* Get an instance of the info plist.*/
			CFDictionaryRef bundleInfoDict= CFBundleGetInfoDictionary(mainBundle);
						
			/* If we succeeded, look for our property. */
			if (bundleInfoDict != NULL) {
				CFArrayRef ar= CFDictionaryGetValue(bundleInfoDict, CFSTR("Eclipse"));
				if (ar) {
					CFIndex size= CFArrayGetCount(ar);
					if (size > 0) {
						int i;
						char **old_argv= argv;
						argv= (char**) calloc(size+2, sizeof(char*));
						argc= 0;
						argv[argc++]= old_argv[0];
						for (i= 0; i < size; i++) {
							CFStringRef sr= (CFStringRef) CFArrayGetValueAtIndex (ar, i);
							CFIndex argStringSize= CFStringGetMaximumSizeForEncoding(CFStringGetLength(sr), kCFStringEncodingUTF8);
							char *s= malloc(argStringSize);
							if (CFStringGetCString(sr, s, argStringSize, kCFStringEncodingUTF8)) {
								argv[argc++]= expandShell(s, fgAppPackagePath, NULL);
							} else {
								fprintf(fgConsoleLog, "can't extract bytes\n");
							}
							//free(s);
						}
					}
				} else {
					fprintf(fgConsoleLog, "no Eclipse dict found\n");
				}
			} else {
				fprintf(fgConsoleLog, "no bundle dict found\n");
			}
		} else {
			fprintf(fgConsoleLog, "no bundle found\n");
		}
	}
	int exitcode= original_main(argc, argv);
	debug("<<<< exit(%d)\n", exitcode);
	fclose(fgConsoleLog);
	return exitcode;
}

/* Display a Message */
void displayMessage(char *title, char *message)
{
	CFStringRef inError, inDescription= NULL;

	/* try to break the message into a first sentence and the rest */
	char *pos= strstr(message, ". ");
	if (pos != NULL) {	
		char *to, *from, *buffer= calloc(pos-message+2, sizeof(char));
		/* copy and replace line separators with blanks */
		for (to= buffer, from= message; from <= pos; from++, to++) {
			char c= *from;
			if (c == '\n') c= ' ';
			*to= c;
		}
		inError= CFStringCreateWithCString(kCFAllocatorDefault, buffer, kCFStringEncodingASCII);
		free(buffer);
		inDescription= CFStringCreateWithCString(kCFAllocatorDefault, pos+2, kCFStringEncodingASCII);
	} else {
		inError= CFStringCreateWithCString(kCFAllocatorDefault, message, kCFStringEncodingASCII);
	}
	
	init();
	
	DialogRef outAlert;
	OSStatus status= CreateStandardAlert(kAlertStopAlert, inError, inDescription, NULL, &outAlert);
	if (status == noErr) {
		DialogItemIndex outItemHit;
		RunStandardAlert(outAlert, NULL, &outItemHit);
	} else {
		debug("%s: displayMessage: %s\n", title, message);
	}
	CFRelease(inError);
	if (inDescription != NULL)
		CFRelease(inDescription);
}

static void debug(const char *fmt, ...) {
#if DEBUG
	va_list ap;
	va_start(ap, fmt);
	fprintf(fgConsoleLog, "%05d: ", fgPid);
	vfprintf(fgConsoleLog, fmt, ap);
	va_end(ap);
#endif
}

static void dumpArgs(char *tag, int argc, char* argv[]) {
#if DEBUG
	int i;
	if (argc < 0) {
		argc= 0;
		for (i= 0; argv[i] != NULL; i++)
			 argc++;
	}
	debug(">>>> %s:", tag);
	for (i= 0; i < argc && argv[i] != NULL; i++)
		fprintf(fgConsoleLog, " <%s>", argv[i]);
	fprintf(fgConsoleLog, "\n");
#endif
}

static void init() {
	static int initialized= 0;
	
	if (!initialized) {
		ProcessSerialNumber psn;
		if (GetCurrentProcess(&psn) == noErr) {
			if (mainEclipse) {
				TransformProcessType(&psn, kProcessTransformToForegroundApplication);
			}
			SetFrontProcess(&psn);
		}
		ClearMenuBar();
		initialized= true;
	}
}

/* Initialize Window System
 *
 * Initialize Carbon.
 */
void initWindowSystem( int* pArgc, char* argv[], int showSplash )
{
	char *homeDir = getProgramDir();
	debug("install dir: %s\n", homeDir);
	if (homeDir != NULL)
		chdir(homeDir);
    
    mainEclipse = !showSplash;
	if (showSplash)
		init();
}

static void eventLoopTimerProc(EventLoopTimerRef inTimer, void *inUserData) {
	QuitApplicationEventLoop();
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

static void readRect(char *str, Rect *rect) {
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
	rect->left = x;
	rect->top = y;
	rect->right = x + width;
	rect->bottom = y + height;
}

static void readColor(char *str, RGBColor *color) {
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
					SetControl32BitMaximum (progress, maximum);
				}
			} else if (strcmp(line, "value") == 0) {
				value = atoi(str);
				if (progress) {
					SetControl32BitValue (progress, value);
				}
			} else if (strcmp(line, "progressRect") == 0) {
				readRect(str, &progressRect);
				if (progress) {
					SetControlBounds (progress, &progressRect);
				}
			} else if (strcmp(line, "messageRect") == 0) {
				readRect(str, &messageRect);
				invalidateWindow();
			} else if (strcmp(line, "foreground") == 0) {
				readColor(str, &foreground);
				invalidateWindow();
			} else if (strcmp(line, "message") == 0) {
				if (string != NULL) CFRelease(string);
				string = CFStringCreateWithBytes (kCFAllocatorDefault, (UInt8 *)str, strlen(str), CFStringGetSystemEncoding(), 1);
				invalidateWindow();
			}
			
		}
		if (end != NULL) line = end + 1;
		else line = NULL;
	}
	free(buffer);
}

static void timerProc(EventLoopTimerRef inTimer, void *inUserData) {
	readInput();
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
int showSplash( char* timeoutString, char* featureImage )
{
	Rect wRect;
	int w, h, deviceWidth, deviceHeight;
	PixMap *pm;
	PixMapHandle pixmap;
	EventTypeSpec draw = {kEventClassControl, 	kEventControlDraw};

	debug("featureImage: %s\n", featureImage);

	init();
    
	/* Determine the splash timeout value (in seconds). */
	if (timeoutString != NULL && strlen(timeoutString) > 0) {
		int timeout;
		if (sscanf(timeoutString, "%d", &timeout) == 1) {
			InstallEventLoopTimer(GetMainEventLoop(), (EventTimerInterval) timeout, 0.0, NewEventLoopTimerUPP(eventLoopTimerProc), NULL, NULL);
		}
	}

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
		readInput();
		InstallEventLoopTimer (GetCurrentEventLoop (), 50 / 1000.0, 50 / 1000.0, NewEventLoopTimerUPP(timerProc), NULL, NULL);
		ShowWindow(window);
		RunApplicationEventLoop();
		DisposeWindow(window);
		if (string != NULL) CFRelease(string);
		string = NULL;
		progress = pane = NULL;
	}

	return 0;
}


/* Get the window system specific VM arguments */
char** getArgVM( char* vm ) 
{
	char** result;

	/* Use the default arguments for a standard Java VM */
	result = argVM_JAVA;

	return result;
}

/* Start the Java VM 
 *
 * This method is called to start the Java virtual machine and to wait until it
 * terminates. The function returns the exit code from the JVM.
 */
int startJavaVM( char* args[] ) 
{
	unsigned int length = 1024, rc;
	int launcherIndex = 0, exitCode;
	char *launcher = NULL, *oldLauncher = NULL, *newLauncher = NULL, *path = args[0];
	
	/*
	 * In order to be able to keep the Java VM in the Dock and restart
	 * it, the first value of the list of arguments passed to execv()
	 * is changed to point back to the Eclipse launcher program. The Eclipse
	 * launcher program is marked as a LSBackgroundOnly app in its Info.plist
	 * file and the secondary launcher used to show the splash screen
	 * is marked as a LSUIElement app. Since it is not possible to change
	 * the Info.plist file dynamically, two application bundles are needed.
	 */
	launcher = malloc(length);
	if ((rc = _NSGetExecutablePath(launcher, &length)) == -1) {
		launcher = realloc(launcher, length);
		rc = _NSGetExecutablePath(launcher, &length);
	}
	if (rc == 0) {
		while (args[launcherIndex] != NULL) {
			if (_tcsicmp( args[ launcherIndex++ ], LAUNCHER ) == 0) {
				char *slash;
				slash = strrchr (launcher, '/');
				if (slash) {
					char * temp;
					*slash = '\0';
					temp = strrchr (launcher, '/');
					*slash = '/';
					slash = temp;
				}
				if (slash) {
					struct _stat stats;
					char* buffer;
					buffer = malloc(strlen(launcher) + strlen(SPLASH_LAUNCHER) + 1);
					*slash = '\0';
					buffer[0] = '\0';
					strcat(buffer, launcher);
					strcat(buffer, SPLASH_LAUNCHER);
					strcat(buffer, slash + 1);
					*slash = '/';
					if (stat(buffer, &stats) == 0) {
						oldLauncher = args[launcherIndex];
						newLauncher = buffer;
						args[launcherIndex] = buffer;
						args[0] = launcher;
					} else {
						free(buffer);
					}
				}
				break;
			}
		}
	}

	/* Create a child process for the JVM. */	
	pid_t pid= fork();
	if (pid == 0) {

		dumpArgs("execv", -1, args);

		/* Child process ... start the JVM */
		execv(path, args);

		/* The JVM would not start ... return error code to parent process. */
		_exit(errno);
	}

	if (pid == -1) {
		exitCode = errno;
	} else {
		/* wait for it to terminate processing events */
		jvmPid = pid;
		InstallEventLoopTimer(GetCurrentEventLoop (), 100 / 1000.0, 100 / 1000.0, NewEventLoopTimerUPP(detectExitTimerProc), NULL, NULL);
		RunApplicationEventLoop();
		exitCode = jvmExitCode;
		exitCode = ((exitCode & 0x00ff) == 0 ? (exitCode >> 8) : exitCode);
	}

	if (newLauncher != NULL) {
		args[0] = path;
		args[launcherIndex] = oldLauncher;
		free(newLauncher);
	}
	if (launcher != NULL) free(launcher);

	return exitCode;
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

/*
 * Expand $APP_PACKAGE, $JAVA_HOME, and does tilde expansion.
 
	A word beginning with an unquoted tilde character (~) is
	subject to tilde expansion. All the characters up to a
	slash (/) or the end of the word are treated as a username
	and are replaced with the user's home directory. If the
	username is missing (as in ~/foobar), the tilde is
	replaced with the value of the HOME variable (the current
	user's home directory).
 */
static char *expandShell(char *arg, const char *appPackage, const char *javaRoot) {
	
	if (index(arg, '~') == NULL && index(arg, '$') == NULL)
		return arg;
	
	char *buffer= strdup("");
	char c, lastChar= ' ';
	const char *cp= arg;
	while ((c = *cp++) != 0) {
		if (isspace(lastChar) && c == '~') {
			char name[100], *dir= NULL;
			int j= 0;
			for (; (c = *cp) != 0; cp++) {
				if (! isalnum(c))
					break;
				name[j++]= c;
				lastChar= c;
			}
			name[j]= '\0';
			if (j > 0) {
				struct passwd *pw= getpwnam(name);
				if (pw != NULL)
					dir= pw->pw_dir;
			} else {
				dir= getenv("HOME");
			}
			if (dir != NULL)
				buffer= append(buffer, dir);
				
		} else if (c == '$') {
			int l= strlen(APP_PACKAGE);
			if (appPackage != NULL && strncmp(cp, APP_PACKAGE, l) == 0) {
				cp+= l;
				buffer= append(buffer, appPackage);
			} else {
				int l= strlen(JAVAROOT);
				if (javaRoot != NULL && strncmp(cp, JAVAROOT, l) == 0) {
					cp+= l;
					buffer= append(buffer, javaRoot);
				} else {
					buffer= appendc(buffer, c);
				}
			}
		} else
			buffer= appendc(buffer, c);
		lastChar= c;
	}
	return buffer;
}

static char *my_strcasestr(const char *big, const char *little) {
    char *cp, *s, *t;
    for (cp= (char*) big; *cp; cp++) {
        for (s= cp, t= (char*) little; *s && *t; s++, t++)
            if (toupper(*s) != toupper(*t))
                break;
        if (*t == '\0')
            return cp;
    }
    return NULL;
}

static char *append(char *buffer, const char *s) {
	int bl= strlen(buffer);
	int sl= strlen(s);
	buffer= realloc(buffer, bl+sl+1);
	strcpy(&buffer[bl], s);
	return buffer;
}

static char *appendc(char *buffer, char c) {
	int bl= strlen(buffer);
	buffer= realloc(buffer, bl+2);
	buffer[bl++]= c;
	buffer[bl]= '\0';
	return buffer;
}

