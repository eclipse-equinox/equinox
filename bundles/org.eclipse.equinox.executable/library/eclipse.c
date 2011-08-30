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
 *	   Markus Schorn (Wind River Systems), bug 193340
 *	   Martin Oberhuber (Wind River) - [149994] Add --launcher.appendVmargs
 *******************************************************************************/

/* Eclipse Program Launcher
 *
 * This file forms the base of the eclipse_*.dll/so.  This dll is loaded by eclipse.exe
 * to start a Java VM, or alternatively it is loaded from Java to show the splash
 * screen or write to the shared memory.  See eclipseJNI.c for descriptions of the methods
 * exposed to the Java program using JNI.
 * 
 * To display a splash screen before starting the java vm, the launcher should be started 
 * with the location of the splash bitmap to use:
 * -showsplash <path/to/splash.bmp>
 * Otherwise, when the Java program starts, it should determine the location of
 * the splash bitmap to be used and use the JNI method show_splash. 
 * 
 * When the Java program initialization is complete, the splash window
 * is brought down by calling the JNI method takedown_splash.
 *
 * The Java program can also call the get_splash_handle method to get the handle to the splash 
 * window.  This can be passed to SWT to create SWT widgets in the splash screen.
 * 
 * The Java application will receive two other arguments:
 *    -exitdata <shared memory id>
 *
 * The java program can call set_exit_data with this shared-memory-id
 * to provide specific exit data to the launcher. 
 *
 * The exit data size must not exceed MAX_SHARED_LENGTH which is
 * 16Kb. The interpretation of the exit data is dependent on the 
 * exit value of the java application.
 *
 * The main launcher recognizes the following exit codes from the
 * Java application:
 *
 *    0    - Exit normally.
 *    RESTART_LAST_EC = 23
 *       - restart the java VM again with the same arguments as the previous one.
 *    RESTART_NEW_EC  = 24
 *       - restart the java VM again with the arguments taken from the exit data.
 *       The exit data format is a list of arguments separated by '\n'. The Java
 *       application should build this list using the arguments passed to it on
 *       startup. See below.
 *
 * Additionally, if the Java application exits with an exit code other than the
 * ones above, the main launcher will display an error message with the contents
 * of the exit data. If the exit data is empty, a generic error message is
 * displayed. The generic error message shows the exit code and the arguments
 * passed to the Java application.
 *
 * The options that can be specified by the user to the launcher are:
 *  -vm <javaVM>               the Java VM to be used
 *  -os <opSys>                the operating system being run on
 *  -arch <osArch>             the hardware architecture of the OS: x86, sparc, hp9000
 *  -ws <gui>                  the window system to be used: win32, motif, gtk, ...
 *  -nosplash                  do not display the splash screen. The java application will
 *                             not receive the -showsplash command.
 *  -showsplash <bitmap>	   show the given bitmap in the splash screen.
 *  -name <name>               application name displayed in error message dialogs and
 *                             splash screen window. Default value is computed from the
 *                             name of the executable - with the first letter capitalized
 *                             if possible. e.g. eclipse.exe defaults to the name Eclipse.
 *  -startup <startup.jar>     the startup jar to execute. The argument is first assumed to be
 *                             relative to the path of the launcher. If such a file does not
 *                             exist, the argument is then treated as an absolute path.
 *                             The default is find the plugins/org.eclipse.equinox.launcher jar
 * 							   with the highest version number.
 *                             The jar must contain an org.eclipse.equinox.launcher.Main class.
 * 							   (unless JNI invocation is not being used, then the jar only needs to be
 * 							   an executable jar)
 * -library					   the location of the eclipse launcher shared library (this library) to use
 * 							   By default, the launcher exe (see eclipseMain.c) finds
 *  <userArgs>                 arguments that are passed along to the Java application
 *                             (i.e, -data <path>, -debug, -console, -consoleLog, etc) 
 *  -vmargs <userVMargs> ...   a list of arguments for the VM itself
 *
 * The -vmargs option and all user specified VM arguments must appear
 * at the end of the command line, after all arguments that are
 * being passed to Java application. 
 *
 * The argument order for the new Java VM process is as follows:
 *
 * <javaVM> <all VM args>
 *     -os <user or default OS value>
 *     -ws <user or default WS value>
 *     -arch <user or default ARCH value>
 *     -launcher <absolute launcher name>
 *     -name <application name>
 * 	   -library <eclipse library location>
 * 	   -startup <startup.jar location>
 *     [-showsplash]
 *     [-exitdata <shared memory id>]
 *     <userArgs>
 *     -vm <javaVM>
 *     -vmargs <all VM args>
 *
 * where:
 *   <all VM args> =
 *     [<defaultVMargs | <userVMargs>]
 *     -jar
 *     <startup jar full path>
 *	
 * The startup jar must be an executable jar.
 * 
 *
 * See "Main.java" for a simple implementation of the Java
 * application.
 *
 * Configuration file
 *   The launcher gets arguments from the command line and/or from a configuration file.
 * The configuration file must have the same name and location as the launcher executable
 * and the extension .ini. For example, the eclipse.ini configuration file must be
 * in the same folder as the eclipse.exe or eclipse executable.
 *   The format of the ini file matches that of the command line arguments - one
 * argument per line.
 *   In general, the settings of the config file are expected to be overriden by the
 * command line.
 *   - launcher arguments (-os, -arch...) set in the config file are overriden by the command line
 *   - the -vmargs from the command line replaces in its entirety the -vmargs from the config file.
 *   - user arguments from the config file are prepended to the user arguments defined in the
 *     config file. This is consistent with the java behaviour in the following case:
 *     java -Dtest="one" -Dtest="two" ...  : test is set to the value "two"
 */

#include "eclipseOS.h"
#include "eclipseUtil.h"
#include "eclipseShm.h"
#include "eclipseJNI.h"
#include "eclipseConfig.h"
#include "eclipseCommon.h"

#ifdef _WIN32
#include <windows.h>
#include <direct.h>
#include <io.h>
#include <fcntl.h>
#else
#include <unistd.h>
#include <strings.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <locale.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <ctype.h>

#define MAX_PATH_LENGTH   2000
#define MAX_SHARED_LENGTH   (16 * 1024)

/* Global Data */
static _TCHAR*  program     = NULL;       /* full pathname of the program */
static _TCHAR*  programDir  = NULL;       /* directory where program resides */
static _TCHAR*  javaVM      = NULL;       /* full pathname of the Java VM to run */
static _TCHAR*  jniLib		= NULL;		  /* full path of a java vm library for JNI invocation */
static _TCHAR*  jarFile     = NULL;		  /* full pathname of the startup jar file to run */
static _TCHAR*  sharedID    = NULL;       /* ID for the shared memory */
static _TCHAR*  officialName  = NULL;

_TCHAR*  exitData    = NULL;		  /* exit data set from Java */
int		 initialArgc;
_TCHAR** initialArgv = NULL;

 	
/* Define the special exit codes returned from Eclipse. */
#define RESTART_LAST_EC    23
#define RESTART_NEW_EC     24

/* constants for launch mode */
#define LAUNCH_JNI 1
#define LAUNCH_EXE 2

#define DEFAULT_EE _T_ECLIPSE("default.ee")

/* Define error messages. (non-NLS) */
static _TCHAR* exitMsg = _T_ECLIPSE("JVM terminated. Exit code=%d\n%s");
static _TCHAR* javaFailureMsg = _T_ECLIPSE("Internal Error, unable to determine the results of running the JVM.");
static _TCHAR* returnCodeMsg = _T_ECLIPSE("Java was started but returned exit code=%d\n%s");
static _TCHAR* goVMMsg = _T_ECLIPSE("Start VM: %s\n");
static _TCHAR* pathMsg = _T_ECLIPSE("%s in your current PATH");
static _TCHAR* shareMsg = _T_ECLIPSE("No exit data available.");
static _TCHAR* noVMMsg =
_T_ECLIPSE("A Java Runtime Environment (JRE) or Java Development Kit (JDK)\n\
must be available in order to run %s. No Java virtual machine\n\
was found after searching the following locations:\n\
%s");
static _TCHAR* startupMsg =
_T_ECLIPSE("The %s executable launcher was unable to locate its \n\
companion launcher jar.");

static _TCHAR* homeMsg =
_T_ECLIPSE("The %s executable launcher was unable to locate its \n\
home directory.");

#define OLD_STARTUP 		_T_ECLIPSE("startup.jar")
#define CLASSPATH_PREFIX        _T_ECLIPSE("-Djava.class.path=")

/* Define constants for the options recognized by the launcher. */
#define CONSOLE      _T_ECLIPSE("-console")
#define CONSOLELOG   _T_ECLIPSE("-consoleLog")
#define DEBUG        _T_ECLIPSE("-debug")
#define OS           _T_ECLIPSE("-os")
#define OSARCH       _T_ECLIPSE("-arch")
#define NOSPLASH     _T_ECLIPSE("-nosplash")
#define LAUNCHER     _T_ECLIPSE("-launcher")
#define SHOWSPLASH   _T_ECLIPSE("-showsplash")
#define EXITDATA     _T_ECLIPSE("-exitdata")
#define STARTUP      _T_ECLIPSE("-startup")
#define VM           _T_ECLIPSE("-vm")
#define WS           _T_ECLIPSE("-ws")
#define NAME         _T_ECLIPSE("-name")
#define VMARGS       _T_ECLIPSE("-vmargs")					/* special option processing required */
#define CP			 _T_ECLIPSE("-cp")
#define CLASSPATH    _T_ECLIPSE("-classpath")
#define JAR 		 _T_ECLIPSE("-jar")

#define OPENFILE	  _T_ECLIPSE("--launcher.openFile")
#define DEFAULTACTION _T_ECLIPSE("--launcher.defaultAction")
#define TIMEOUT		  _T_ECLIPSE("--launcher.timeout")
#define LIBRARY		  _T_ECLIPSE("--launcher.library")
#define SUPRESSERRORS _T_ECLIPSE("--launcher.suppressErrors")
#define INI			  _T_ECLIPSE("--launcher.ini")
#define APPEND_VMARGS _T_ECLIPSE("--launcher.appendVmargs")
#define OVERRIDE_VMARGS _T_ECLIPSE("--launcher.overrideVmargs")
#define SECOND_THREAD _T_ECLIPSE("--launcher.secondThread")
#define PERM_GEN	  _T_ECLIPSE("--launcher.XXMaxPermSize")

#define XXPERMGEN	  _T_ECLIPSE("-XX:MaxPermSize=")
#define ACTION_OPENFILE _T_ECLIPSE("openFile")

/* constants for ee options file */
#define EE_EXECUTABLE 			_T_ECLIPSE("-Dee.executable=")
#define EE_CONSOLE 	_T_ECLIPSE("-Dee.executable.console=")
#define EE_VM_LIBRARY			_T_ECLIPSE("-Dee.vm.library=")
#define EE_LIBRARY_PATH			_T_ECLIPSE("-Dee.library.path=")
#define EE_HOME					_T_ECLIPSE("-Dee.home=")
#define EE_FILENAME				_T_ECLIPSE("-Dee.filename=")
#define EE_HOME_VAR				_T_ECLIPSE("${ee.home}")

/* Define the variables to receive the option values. */
static int     needConsole   = 0;				/* True: user wants a console	*/
static int     debug         = 0;				/* True: output debugging info	*/
static int     noSplash      = 0;				/* True: do not show splash win	*/
static int	   suppressErrors = 0;				/* True: do not display errors dialogs */
       int     secondThread  = 0;				/* True: start the VM on a second thread */
static int     appendVmargs = 0;                /* True: append cmdline vmargs to launcher.ini vmargs */
       
static _TCHAR*  showSplashArg = NULL;			/* showsplash data (main launcher window) */
static _TCHAR*  splashBitmap  = NULL;			/* the actual splash bitmap */
static _TCHAR * startupArg    = NULL;			/* path of the startup.jar the user wants to run relative to the program path */
static _TCHAR*  vmName        = NULL;     		/* Java VM that the user wants to run */
static _TCHAR*  name          = NULL;			/* program name */	
static _TCHAR*  permGen  	  = NULL;			/* perm gen size for sun */
static _TCHAR**  filePath	  = NULL;			/* list of files to open */
static _TCHAR*  timeoutString = NULL;			/* timeout value for opening a file */
static _TCHAR*  defaultAction = NULL;			/* default action for non '-' command line arguments */ 
static _TCHAR*  iniFile       = NULL;			/* the launcher.ini file set if  --launcher.ini was specified */

/* variables for ee options */
static _TCHAR* eeExecutable = NULL;
static _TCHAR* eeConsole = NULL;
static _TCHAR* eeLibrary = NULL;

_TCHAR* eeLibPath = NULL;			/* this one is global so others can see it */
_TCHAR*  eclipseLibrary = NULL;	/* the shared library */

/* Define a table for processing command line options. */
typedef struct
{
	_TCHAR*  name;		/* the option recognized by the launcher */
	void*  value;		/* the variable where the option value is saved */
						/* value is a _TCHAR** or int* depending on if VALUE_IS_FLAG is set */
	int    flag;		/* flags */
	int    remove;		/* the number of argments to remove from the list, -1 can be used with VALUE_IS_LIST */
} Option;

/* flags for the Option struct */
#define VALUE_IS_FLAG 	1   /* value is an int*, if not set, value is a _TCHAR** or _TCHAR*** (VALUE_IS_LIST) */
#define OPTIONAL_VALUE  2  	/* value is optional, if next arg does not start with '-', */
							/* don't assign it and only remove (remove - 1) arguments  */
#define ADJUST_PATH		4  	/* value is a path, do processing on relative paths to try and make them absolute */
#define VALUE_IS_LIST	8  	/* value is a pointer to a tokenized _TCHAR* string for EE files, or a _TCHAR** list for the command line */
#define INVERT_FLAG    16   /* invert the meaning of a flag, i.e. reset it */

static Option options[] = {
    { CONSOLE,		&needConsole,	VALUE_IS_FLAG,	0 },
    { CONSOLELOG,	&needConsole,	VALUE_IS_FLAG,	0 },
    { DEBUG,		&debug,			VALUE_IS_FLAG,	0 },
    { NOSPLASH,     &noSplash,      VALUE_IS_FLAG,	1 },
    { SUPRESSERRORS, &suppressErrors, VALUE_IS_FLAG, 1},
    { SECOND_THREAD, &secondThread, VALUE_IS_FLAG,  1 },
    { APPEND_VMARGS, &appendVmargs,	VALUE_IS_FLAG, 1 },
    { OVERRIDE_VMARGS, &appendVmargs, VALUE_IS_FLAG | INVERT_FLAG, 1 },
    { LIBRARY,		NULL,			0,			2 }, /* library was parsed by exe, just remove it */
    { INI,			&iniFile, 		0,			2 },
    { OS,			&osArg,			0,			2 },
    { OSARCH,		&osArchArg,		0,			2 },
    { SHOWSPLASH,   &showSplashArg,	OPTIONAL_VALUE,	2 },
    { STARTUP,		&startupArg,	0,			2 },
    { VM,           &vmName,		0,			2 },
    { NAME,         &name,			0,			2 },
    { PERM_GEN,		&permGen,		0,			2 },
    { OPENFILE,		&filePath,		ADJUST_PATH | VALUE_IS_LIST, -1 },
    { TIMEOUT,		&timeoutString, 0,          2 },
    { DEFAULTACTION,&defaultAction, 0,			2 },
    { WS,			&wsArg,			0,			2 } };
static int optionsSize = (sizeof(options) / sizeof(options[0]));

static Option eeOptions[] = {
	{ EE_EXECUTABLE,	&eeExecutable, 	ADJUST_PATH, 0 },
	{ EE_CONSOLE,	 	&eeConsole,		ADJUST_PATH, 0 },
	{ EE_VM_LIBRARY, 	&eeLibrary,		ADJUST_PATH, 0 },
	{ EE_LIBRARY_PATH,	&eeLibPath, 	ADJUST_PATH | VALUE_IS_LIST, 0 }
};
static int eeOptionsSize = (sizeof(eeOptions) / sizeof(eeOptions[0]));

/* Define the required VM arguments (all platforms). */
static _TCHAR*  cp = NULL;
static _TCHAR*  cpValue = NULL;					
static _TCHAR** reqVMarg[] = { &cp, &cpValue, NULL };	/* required VM args */
static _TCHAR** userVMarg  = NULL;	     				/* user specific args for the Java VM  */
static _TCHAR** eeVMarg = NULL;							/* vm args specified in ee file */
static int nEEargs = 0;

/* Local methods */
static void     parseArgs( int* argc, _TCHAR* argv[] );
static void 	processDefaultAction(int argc, _TCHAR* argv[]);
static void 	mergeUserVMArgs( _TCHAR **vmArgs[] );
static void     getVMCommand( int launchMode, int argc, _TCHAR* argv[], _TCHAR **vmArgv[], _TCHAR **progArgv[] );
static int 		determineVM(_TCHAR** msg);
static int 		vmEEProps(_TCHAR* eeFile, _TCHAR** msg);
static int 		processEEProps(_TCHAR* eeFile);
static _TCHAR** buildLaunchCommand( _TCHAR* program, _TCHAR** vmArgs, _TCHAR** progArgs );
static _TCHAR** parseArgList( _TCHAR *data );
static _TCHAR*  formatVmCommandMsg( _TCHAR* args[], _TCHAR* vmArgs[], _TCHAR* progArgs[] );
static _TCHAR*  getDefaultOfficialName();
static _TCHAR*  findStartupJar();
static _TCHAR*  findSplash(_TCHAR* splashArg);
static _TCHAR** getRelaunchCommand( _TCHAR **vmCommand );
static const _TCHAR* getVMArch();

#ifdef _WIN32
static void     createConsole();
static void		fixDLLSearchPath();
static int 		isConsoleLauncher();
#endif
static int      consoleLauncher = 0;

/* Record the arguments that were used to start the original executable */
JNIEXPORT void setInitialArgs(int argc, _TCHAR** argv, _TCHAR* lib) {
	initialArgc = argc;
	initialArgv = argv;
	eclipseLibrary = lib;
}

/* this method must match the RunMethod typedef in eclipseMain.c */
/* vmArgs must be NULL terminated                                */
JNIEXPORT int run(int argc, _TCHAR* argv[], _TCHAR* vmArgs[])
{
    _TCHAR**  vmCommand = NULL;
    _TCHAR**  vmCommandArgs = NULL;
    _TCHAR**  progCommandArgs = NULL;
    _TCHAR**  relaunchCommand = NULL;
    _TCHAR*   errorMsg = NULL, *msg = NULL;
    JavaResults* javaResults = NULL;
    int 	  launchMode;
    int 	  running = 1;
	 
	/* arg[0] should be the full pathname of this program. */
    program = _tcsdup( argv[0] );
	
    /* Parse command line arguments (looking for the VM to use). */
    /* Override configuration file arguments */
    parseArgs( &argc, argv );

	/* Initialize official program name */
   	officialName = name != NULL ? _tcsdup( name ) : getDefaultOfficialName();
    
   	if (defaultAction != NULL) {
   		processDefaultAction(initialArgc, initialArgv);
   	}
   	
   	/* try to open the specified file in an already running eclipse */
   	/* on Mac we are only registering an event handler here, always do this */
#ifndef MACOSX
	if (filePath != NULL && filePath[0] != NULL)
#endif
	{
		int timeout = 60;
		if (timeoutString != NULL)
			_stscanf(timeoutString, _T_ECLIPSE("%d"), &timeout);
		if (reuseWorkbench(filePath, timeout) > 0)
			return 0;
	}

#ifdef MACOSX
   	/* Most platforms, we will initialize the window system later before trying to do any
   	 * graphics.  On Mac, we need it initialized to get the dock icon properly, so always do 
   	 * it now.
   	 */
    initWindowSystem( &argc, argv, !noSplash );
#elif _WIN32
    /* this must be before doing any console stuff, platforms other than win32 leave this set to 0 */
    consoleLauncher = isConsoleLauncher();
    
    /*fix the DLL search path for security */
    fixDLLSearchPath();
#endif
    
    /* Find the directory where the Eclipse program is installed. */
    programDir = getProgramDir();
    if (programDir == NULL)
    {
        errorMsg = malloc( (_tcslen(homeMsg) + _tcslen(officialName) + 10) * sizeof(_TCHAR) );
        _stprintf( errorMsg, homeMsg, officialName );
        if (!suppressErrors)
        	displayMessage( officialName, errorMsg );
        else
        	_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, errorMsg); 
        free( errorMsg );
    	exit( 1 );
    }

    if (vmArgs != NULL) {
    	/* reconcile VM Args from commandline with launcher.ini (append or override),
    	 * this always allocates new memory */
    	mergeUserVMArgs(&vmArgs);
    	/* platform specific processing of user's vmargs */
    	processVMArgs(&vmArgs);
    }
    
    launchMode = determineVM(&msg);
    if (launchMode == -1) {
    	/* problem */
    	errorMsg = malloc((_tcslen(noVMMsg) + _tcslen(officialName) + _tcslen(msg) + 1) * sizeof(_TCHAR));
    	_stprintf( errorMsg, noVMMsg, officialName, msg );
    	if (!suppressErrors)
    		displayMessage( officialName, errorMsg );
    	else
    		_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, errorMsg);
    	free( errorMsg );
    	free( msg );
    	exit(1);
	}	
	
	/* Find the startup.jar */
	jarFile = findStartupJar();
	if(jarFile == NULL) {
		errorMsg = malloc( (_tcslen(startupMsg) + _tcslen(officialName) + 10) * sizeof(_TCHAR) );
        _stprintf( errorMsg, startupMsg, officialName );
        if (!suppressErrors)
    		displayMessage( officialName, errorMsg );
    	else
    		_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, errorMsg);
        free( errorMsg );
    	exit( 1 );
	}

#ifdef _WIN32
	if( launchMode == LAUNCH_JNI && (debug || needConsole) ) {
		createConsole();
	}
#endif

    /* If the showsplash option was given and we are using JNI */
    if (!noSplash && showSplashArg)
    {
    	splashBitmap = findSplash(showSplashArg);
    	if (splashBitmap != NULL && launchMode == LAUNCH_JNI) {
	    	showSplash(splashBitmap);
    	}
    }
    
    /* not using JNI launching, need some shared data */
    if (launchMode == LAUNCH_EXE && createSharedData( &sharedID, MAX_SHARED_LENGTH )) {
        if (debug) {
        	if (!suppressErrors) 
        		displayMessage( officialName, shareMsg );
        	else
           		_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, shareMsg);
        }
    }
    
	/* the startup jarFile goes on the classpath */
    if (launchMode == LAUNCH_JNI) {
    	/* JNI launching, classpath is set using -Djava.class.path */
		cp = malloc((_tcslen(CLASSPATH_PREFIX) + _tcslen(jarFile) + 1) * sizeof(_TCHAR));
		cp = _tcscpy(cp, CLASSPATH_PREFIX);
		_tcscat(cp, jarFile);
    } else {
    	/* exec java, jar is specified with -jar */
    	cp = JAR;
    	cpValue = malloc((_tcslen(jarFile) + 1) * sizeof(_TCHAR));
    	_tcscpy(cpValue, jarFile);
    }
	
    /* Get the command to start the Java VM. */
    userVMarg = vmArgs;
    getVMCommand( launchMode, argc, argv, &vmCommandArgs, &progCommandArgs );
	
    if (launchMode == LAUNCH_EXE) {
    	vmCommand = buildLaunchCommand(javaVM, vmCommandArgs, progCommandArgs);
    }
    
    /* While the Java VM should be restarted */
    while(running)
    {
		msg = formatVmCommandMsg( vmCommand, vmCommandArgs, progCommandArgs );
		if (debug) _tprintf( goVMMsg, msg );

		if(launchMode == LAUNCH_JNI) {
			javaResults = startJavaVM(jniLib, vmCommandArgs, progCommandArgs, jarFile);
		} else {
			javaResults = launchJavaVM(vmCommand);
		}
		
		if (javaResults == NULL) {
			/* shouldn't happen, but just in case */
			javaResults = malloc(sizeof(JavaResults));
			javaResults->launchResult = -11;
			javaResults->runResult = 0;
			javaResults->errorMessage = _tcsdup(javaFailureMsg);
		}
		
	    switch( javaResults->launchResult + javaResults->runResult ) {
	        case 0: /* normal exit */
	        	running = 0;
	            break;
	        case RESTART_LAST_EC:
	        	if (launchMode == LAUNCH_JNI) {
		        	/* copy for relaunch, +1 to ensure NULL terminated */
		        	relaunchCommand = malloc((initialArgc + 1) * sizeof(_TCHAR*));
		        	memcpy(relaunchCommand, initialArgv, (initialArgc + 1) * sizeof(_TCHAR*));
		        	relaunchCommand[initialArgc] = 0;
		        	relaunchCommand[0] = program;
		        	running = 0;
	        	}
	        	break;
	        	
	        case RESTART_NEW_EC:
	        	if(launchMode == LAUNCH_EXE) {
	        		if (exitData != NULL) free(exitData);
	        		if (getSharedData( sharedID, &exitData ) != 0)
	        			exitData = NULL;
	        	}
	            if (exitData != 0) {
	            	if (vmCommand != NULL) free( vmCommand );
	                vmCommand = parseArgList( exitData );
	                if (launchMode == LAUNCH_JNI) {
	                	relaunchCommand = getRelaunchCommand(vmCommand);
	                	running = 0;
	                }
	            } else {
	            	running = 0;
	                if (debug) {
	                	if (!suppressErrors) 
        	        		displayMessage( officialName, shareMsg );
        	        	else
        	           		_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), officialName, shareMsg);
	                }
	            }
	            break;
			default: {
				_TCHAR *title = _tcsdup(officialName);
	            running = 0;
	            errorMsg = NULL;
	            if (launchMode == LAUNCH_EXE) {
	            	if (exitData != NULL) free(exitData);
	        		if (getSharedData( sharedID, &exitData ) != 0)
	        			exitData = NULL;
	        	}
	            if (exitData != 0) {
	            	errorMsg = exitData;
	            	exitData = NULL;
	                if (_tcslen( errorMsg ) > 0) {
	                    _TCHAR *str;
	                	if (_tcsncmp(errorMsg, _T_ECLIPSE("<title>"), _tcslen(_T_ECLIPSE("<title>"))) == 0) {
							str = _tcsstr(errorMsg, _T_ECLIPSE("</title>"));
							if (str != NULL) {
								free( title );
								str[0] = _T_ECLIPSE('\0');
								title = _tcsdup( errorMsg + _tcslen(_T_ECLIPSE("<title>")) );
								str = _tcsdup( str + _tcslen(_T_ECLIPSE("</title>")) );
								free( errorMsg );
								errorMsg = str;
							}
	                	}
	                }
	            } else {
	            	 if (debug) {
	                	if (!suppressErrors) 
        	        		displayMessage( title, shareMsg );
        	        	else
        	           		_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), title, shareMsg);
	                }
	            }
	            if (errorMsg == NULL) {
	            	if (javaResults->runResult) {
	            		/* java was started ok, but returned non-zero exit code */
	            		errorMsg = malloc( (_tcslen(returnCodeMsg) + _tcslen(msg) + 10) *sizeof(_TCHAR));
	            		_stprintf(errorMsg, returnCodeMsg,javaResults->runResult, msg);
	            	} else if (javaResults->errorMessage != NULL){
	            		/* else we had a problem launching java, use custom error message */
	            		errorMsg = javaResults->errorMessage;
	            	} else {
	            		/* no custom message, use generic message */
						errorMsg = malloc( (_tcslen(exitMsg) + _tcslen(msg) + 10) * sizeof(_TCHAR) );
						_stprintf( errorMsg, exitMsg, javaResults->launchResult, msg );
	            	}
	            }
	            
	            if (_tcslen(errorMsg) > 0) {
		            if (!suppressErrors)
		            	displayMessage( title, errorMsg );
		            else 
		            	_ftprintf(stderr, _T_ECLIPSE("%s:\n%s\n"), title, errorMsg);
	            }
	            free( errorMsg );
	            free( title );
	            break;
	        }
	    }
	    free( msg );
    }

    if(relaunchCommand != NULL)
    	restartLauncher(NULL, relaunchCommand);
    	
    if (launchMode == LAUNCH_JNI)
    	cleanupVM(javaResults->launchResult ? javaResults->launchResult : javaResults->runResult);
    
    if (sharedID != NULL) {
    	destroySharedData( sharedID );
    	free( sharedID );
    }
    
    /* Cleanup time. */
    free( vmCommandArgs );
    free( progCommandArgs );
    free( jarFile );
    free( programDir );
    free( program );
    free( officialName );
    if(vmCommand != NULL)	 	 free(vmCommand);
    if(launchMode == LAUNCH_JNI) free(cp);
    if(cpValue != NULL)		 	 free(cpValue);
    if(exitData != NULL)		 free(exitData);
    if(splashBitmap != NULL)  	 free(splashBitmap);
    if(vmArgs != NULL)			 free(vmArgs);

    if (javaResults == NULL)
    	return -1;
    
    /* reuse the running variable for convenience */
    running = javaResults->launchResult != 0 ? javaResults->launchResult : javaResults->runResult;
    free(javaResults);
    return running;
}

static _TCHAR** buildLaunchCommand( _TCHAR* program, _TCHAR** vmArgs, _TCHAR** progArgs ) {
	int nVM = -1, nProg = -1;
	_TCHAR** result;
	
	while(vmArgs[++nVM] != NULL) {}
	while(progArgs[++nProg] != NULL) {}
	
	result = malloc((nVM + nProg + 2) * sizeof(_TCHAR*));
	memset(result, 0, (nVM + nProg + 2) * sizeof(_TCHAR*));
	result[0] = program;
	memcpy(result + 1, vmArgs, nVM * sizeof(_TCHAR*));
	memcpy(result + 1 + nVM, progArgs, nProg * sizeof(_TCHAR*));
	return result;
}

static void processDefaultAction(int argc, _TCHAR* argv[]) {
	/* scan the arg list, no default if any start with '-' */
	int i = 0;
	for (i = 0; i < argc; i++) {
		if (argv[i][0] == _T_ECLIPSE('-'))
			return;
	}
	/* argv[0] is the program (eclipse),  we process the default actions by inserting
	 * the appropriate -argument at argv[1]
	 */
	if (argc <= 1)
		return;

	if (_tcsicmp(defaultAction, ACTION_OPENFILE) == 0) {
		int newArgc = argc + 1;
		_TCHAR ** newArgv = malloc((newArgc + 1) * sizeof(_TCHAR*));
		newArgv[0] = argv[0];
		newArgv[1] = OPENFILE;
		memcpy(&newArgv[2], &argv[1], argc * sizeof(_TCHAR*));
		parseArgs(&newArgc, newArgv);
		free(newArgv);
	}
}

/*
 * Parse arguments of the command.
 */
static void parseArgs(int* pArgc, _TCHAR* argv[]) {
	Option* option;
	int remArgs;
	int index;
	int i;
	_TCHAR * c;

	/* For each user defined argument (excluding the program) */
	for (index = 1; index < *pArgc; index++) {
		remArgs = 0;

		/* Find the corresponding argument is a option supported by the launcher */
		option = NULL;
		for (i = 0; option == NULL && i < optionsSize; i++) {
			if (_tcsicmp(argv[index], options[i].name) == 0) {
				option = &options[i];
				break;
			}
		}

		/* If the option is recognized by the launcher */
		if (option != NULL) {
			int optional = 0;
			c = option->name;
			/* If the option requires a value and there is one, extract the value. */
			if (option->value != NULL) {
				if (option->flag & VALUE_IS_FLAG)
					*((int *) option->value) = (option->flag & INVERT_FLAG) ? 0 : 1;
				else {
					int count = 1;
					if (option->flag & VALUE_IS_LIST) {
						/* count how many args, this is the -argument itself + following the non'-' args */
						while (count + index < *pArgc && argv[count + index][0] != _T_ECLIPSE('-'))
							count++;

						/* allocate memory for a _TCHAR* list and initialize it with NULLs*/
						*((void**) option->value) = malloc(count * sizeof(_TCHAR *));
						memset(*((void **) option->value), 0, count * sizeof(_TCHAR *));

						if (option->remove != 0)
							option->remove = count;
					}

					for (i = 0; i < count; i++) {
						if ((index + i + 1) < *pArgc) {
							_TCHAR * next = argv[index + i + 1];
							if (option->flag & ADJUST_PATH)
								next = checkPath(next, getProgramDir(), 0);
							if (next[0] != _T_ECLIPSE('-')) {
								if (option->flag & VALUE_IS_LIST)
									(*((_TCHAR***) option->value))[i] = next;
								else
									*((_TCHAR**) option->value) = next;
							} else if (option->flag & OPTIONAL_VALUE) {
								/* value was optional, and the next arg starts with '-' */
								optional = 1;
							}
						}
					}
				}
			}

			/* If the option requires a flag to be set, set it. */
			remArgs = option->remove - optional;
		}

		/* Remove any matched arguments from the list. */
		if (remArgs > 0) {
			for (i = (index + remArgs); i <= *pArgc; i++) {
				argv[i - remArgs] = argv[i];
			}
			index--;
			*pArgc -= remArgs;
		}
	}
}

/*
 * Parse the data into a list of arguments separated by \n.
 */
static _TCHAR** parseArgList( _TCHAR* data ) {
    int totalArgs = 0, dst = 0;
    size_t length;
    _TCHAR *ch1, *ch2, **execArg;
    length = _tcslen( data );
    ch1 = ch2 = data;
    while ((ch2 = _tcschr( ch1, _T_ECLIPSE('\n') )) != NULL) {
    	totalArgs++;
    	ch1 = ch2 + 1;
    }
    if (ch1 != data + length) totalArgs++;
    execArg = malloc( (totalArgs + 1) * sizeof( _TCHAR* ) );
    ch1 = ch2 = data;
    while ((ch2 = _tcschr( ch1, _T_ECLIPSE('\n') )) != NULL) {
    	execArg[ dst++ ] = ch1;
    	ch2[ 0 ] = _T_ECLIPSE('\0');
    	ch1 = ch2 + 1;
    }
    if (ch1 != data + length) execArg[ dst++ ] = ch1;
    execArg[ dst++ ] = NULL;
    return execArg;
}

/* Return the list of args from the launcher ini file (if it exists). Caller is responsible to free(). */
static _TCHAR** getConfigArgs() {
	_TCHAR** configArgv = NULL;
	_TCHAR * configFile = NULL;
	int configArgc = 0;
	int ret = 0;

	configFile = (iniFile != NULL) ? iniFile : getIniFile(program, consoleLauncher);
	ret = readConfigFile(configFile, &configArgc, &configArgv);
	if (ret == 0)
		return configArgv;
	return NULL;
}

/** Append Commandline VM Args to VM Args that came from the launcher.ini
 *  Always returns new memory even if no new arguments were appended */
static void mergeUserVMArgs(_TCHAR **vmArgs[]) {
	_TCHAR** configVMArgs = NULL;
	_TCHAR** configArgs = NULL;

	if (appendVmargs != 0 && indexOf(VMARGS, initialArgv) > 0) {
		/* Get vmargs from the launcher.ini, if any */
		configArgs = getConfigArgs();
		if (configArgs != NULL) {
			int vmArg = indexOf(VMARGS, configArgs);
			if (vmArg >= 0)
				configVMArgs = configArgs + vmArg + 1;
		}
	}

	/* This always allocates new memory so we don't need to guess if it is safe
	 * to free later  */
	*vmArgs = concatArgs(configVMArgs, *vmArgs);
	if (configArgs != NULL)
		free(configArgs);
}

static void adjustVMArgs(_TCHAR *javaVM, _TCHAR *jniLib, _TCHAR **vmArgv[]) {
	/* Sun VMs need some extra perm gen space */
	/* Detecting Sun VM is expensive - only do so if necessary */
	if (permGen != NULL) {
		int specified = 0, i = -1;

		/* first check to see if it is already specified */
		while ((*vmArgv)[++i] != NULL) {
			/* we are also counting the number of args here */
			if (!specified && _tcsncmp((*vmArgv)[i], XXPERMGEN, _tcslen(XXPERMGEN)) == 0) {
				specified = 1;
			}
		}

		if (!specified && isSunVM(javaVM, jniLib)) {
			_TCHAR ** oldArgs = *vmArgv;
			_TCHAR *newArg = malloc((_tcslen(XXPERMGEN) + _tcslen(permGen) + 1) * sizeof(_TCHAR));
			_stprintf(newArg, _T_ECLIPSE("%s%s"), XXPERMGEN, permGen);

			*vmArgv = malloc((i + 2) * sizeof(_TCHAR *));
			memcpy(*vmArgv, oldArgs, i * sizeof(_TCHAR *));
			(*vmArgv)[i] = newArg;
			(*vmArgv)[i + 1] = 0;
		}
	}
}

/*
 * Get the command and arguments to start the Java VM.
 *
 * Memory allocated by this function is assumed to be
 * deallocated when the program terminates.
 *
 * Some of the arguments returned by this function were
 * passed directly from the main( argv ) array so they
 * should not be deallocated.
 * 
 * Arguments are split into 2: vm arguments and program arguments
 */
static void getVMCommand( int launchMode, int argc, _TCHAR* argv[], _TCHAR **vmArgv[], _TCHAR **progArgv[] )
{
	_TCHAR** vmArg;
    int     nReqVMarg = 0;
    int     nVMarg = 0;
    int     totalVMArgs;
    int		totalProgArgs;
    int     src;
    int     dst;

	/* If the user specified "-vmargs", add them instead of the default VM args. */
	vmArg = (userVMarg != NULL) ? userVMarg : getArgVM( (launchMode == LAUNCH_JNI) ? jniLib : javaVM ); 
 	
	adjustVMArgs(javaVM, jniLib, &vmArg);
	
 	/* Calculate the number of VM arguments. */
 	while (vmArg[ nVMarg ] != NULL)
 		nVMarg++;

 	/* Calculate the number of required VM arguments. */
 	while (reqVMarg[ nReqVMarg ] != NULL)
 		nReqVMarg++;

	/* VM argument list */
	totalVMArgs = nVMarg + nReqVMarg + nEEargs + 1;
	*vmArgv = malloc( totalVMArgs * sizeof(_TCHAR*) );
	
	dst = 0;
	for (src = 0; src < nVMarg; src++){
		/*if the user specified a classpath, skip it */
		if(_tcscmp(vmArg[src], cp) == 0){
			src++;
			continue;
		}
    	(*vmArgv)[ dst++ ] = vmArg[ src ];
	}

	if (eeVMarg != NULL)
		for (src = 0; src < nEEargs; src++)
			(*vmArgv)[ dst++ ] = eeVMarg[ src ];
	
	/* For each required VM arg */
	for (src = 0; src < nReqVMarg; src++)
		if( *(reqVMarg[src]) != NULL)
			(*vmArgv)[ dst++ ] = *(reqVMarg[ src ]);
	
	
	(*vmArgv)[dst] = NULL;
	
	/* Program arguments */
    /*  OS <os> + WS <ws> + ARCH <arch> + LAUNCHER <launcher> + NAME <officialName> +
     *  + LIBRARY <library> + SHOWSPLASH <cmd> + EXITDATA <cmd> + STARTUP <jar> + OVERRIDE/APPEND + argv[] + VM + <vm> +
     * VMARGS + vmArg + requiredVMargs
     *  + NULL)
     */
    totalProgArgs  = 2 + 2 + 2 + 2 + 2 + 2 + 2 + 2 + 2 + 1 + argc + 2 + 1 + nVMarg + nEEargs + nReqVMarg + 1;
	*progArgv = malloc( totalProgArgs * sizeof( _TCHAR* ) );
    dst = 0;
    
    /* Append the required options. */
    (*progArgv)[ dst++ ] = OS;
    (*progArgv)[ dst++ ] = osArg;
    (*progArgv)[ dst++ ] = WS;
    (*progArgv)[ dst++ ] = wsArg;
    if (_tcslen(osArchArg) > 0) {
        (*progArgv)[ dst++ ] = OSARCH;
        (*progArgv)[ dst++ ] = osArchArg;
    }

	/* Append the show splash window command, if defined. */
    if (!noSplash)
    {
        (*progArgv)[ dst++ ] = SHOWSPLASH;
        if(splashBitmap != NULL)
        	(*progArgv)[ dst++ ] = splashBitmap;
    }
    
	/* Append the launcher command */
	(*progArgv)[ dst++ ] = LAUNCHER;
	(*progArgv)[ dst++ ] = program;

	/* Append the name command */
	(*progArgv)[ dst++ ] = NAME;
	(*progArgv)[ dst++ ] = 	officialName;
	
	/* And the shared library */
	if (eclipseLibrary != NULL) {
		(*progArgv)[ dst++ ] = LIBRARY;
		(*progArgv)[ dst++ ] = eclipseLibrary;
	}
	
	/* the startup jar */
	(*progArgv)[ dst++ ] = STARTUP;
	(*progArgv)[ dst++ ] = jarFile;
    
	/* override or append vm args */
	(*progArgv)[ dst++ ] = appendVmargs ? APPEND_VMARGS : OVERRIDE_VMARGS;

	/* Append the exit data command. */
	if (sharedID) {
		(*progArgv)[ dst++ ] = EXITDATA;
		(*progArgv)[ dst++ ] = sharedID;
	}

	/* Append the remaining user defined arguments. */
    for (src = 1; src < argc; src++)
    {
        (*progArgv)[ dst++ ] = argv[ src ];
    }

    /* Append VM and VMARGS to be able to relaunch using exit data. */
	(*progArgv)[ dst++ ] = VM;
	if(jniLib != NULL)
		(*progArgv)[ dst++ ] = jniLib;
	else
		(*progArgv)[ dst++ ] = javaVM;
    (*progArgv)[ dst++ ] = VMARGS;
    
	for (src = 0; src < nVMarg; src++)
    	(*progArgv)[ dst++ ] = vmArg[ src ];
	
	if (eeVMarg != NULL)
		for (src = 0; src < nEEargs; src++)
			(*progArgv)[ dst++ ] = eeVMarg[ src ];
	
    /* For each required VM arg */
    for (src = 0; src < nReqVMarg; src++)
    	if (*(reqVMarg[src]) != NULL)
    		(*progArgv)[ dst++ ] = *(reqVMarg[ src ]);

    (*progArgv)[ dst++ ] = NULL;

 }

 /* Format the JVM start command for error messages
  *
  * This method formats a string with the JVM start command (and all arguments)
  * that can be used in displaying error messages. The string returned from this
  * method is probably not NLS compliant and must be deallocated by the caller.
  * 
  * The arguments in the message are either args (if not null) or the combination 
  * of vmArgs + progArgs
  */
static _TCHAR*  formatVmCommandMsg( _TCHAR* args[], _TCHAR* vmArgs[], _TCHAR* progArgs[] )
{
	int   index;
    size_t length = 0;
    _TCHAR** list;
    _TCHAR* ch;
    _TCHAR* message;

	/* Determine the length of the message buffer. */
	if(args != NULL) list = args;
	else             list = vmArgs;
	while(list != NULL) {
		for (index = 0; list[index] != NULL; index++)
		{
			length += _tcslen(list[index]) + 1;
		}
		if(list == vmArgs) list = progArgs;
		else 			   list = NULL;
	}
	message = malloc( (length + 5) * sizeof(_TCHAR) );
	
	/* Format the message such that options (args starting with '-') begin
	   on a new line. Otherwise, the Motif MessageBox does not automatically wrap
	   the messages and the message window can extend beyond both sides of the display. */
	ch = message;
	if(args != NULL) list = args;
	else             list = vmArgs;
	while(list != NULL) {
		for (index = 0; list[index] != NULL; index++)
		{
			if (ch != message && list[index][0] == _T_ECLIPSE('-') && *(ch-1) == _T_ECLIPSE(' '))
				*(ch-1) = _T_ECLIPSE('\n');
			_tcscpy( ch, list[index] );
			ch += _tcslen( list[index] );
			*ch++ = _T_ECLIPSE(' ');
		}
		if(list == vmArgs) list = progArgs;
		else 			   list = NULL;
	}
	*ch = _T_ECLIPSE('\0');

	return message;
}

_TCHAR* getOfficialName() {
	return officialName;
}

void setOfficialName(_TCHAR* name) {
	officialName = name;
}

_TCHAR* getProgramPath() {
	return program;
}

void setProgramPath(_TCHAR* path) {
	program = path;
}

/*
 * Determine the default official application name
 *
 * This function provides the default application name that appears in a variety of
 * places such as: title of message dialog, title of splash screen window
 * that shows up in Windows task bar.
 * It is computed from the name of the launcher executable and
 * by capitalizing the first letter. e.g. "c:/ide/eclipse.exe" provides
 * a default name of "Eclipse".
 */
static _TCHAR* getDefaultOfficialName()
{
	_TCHAR *ch = NULL;
	
	/* Skip the directory part */
	ch = lastDirSeparator( program );
	if (ch == NULL) ch = program;
	else ch++;
	
	ch = _tcsdup( ch );
#ifdef _WIN32
	{
		/* Search for the extension .exe and cut it */
		_TCHAR *extension = _tcsrchr(ch, _T_ECLIPSE('.'));
		if (extension != NULL) 
		{
			*extension = _T_ECLIPSE('\0');
		}
	}
#endif
	/* Upper case the first character */
#ifndef LINUX
	{
		*ch = _totupper(*ch);
	}
#else
	{
		if (*ch >= 'a' && *ch <= 'z')
		{
			*ch -= 32;
		}
	}
#endif
	return ch;
}

/* Determine the Program Directory
 *
 * This function takes the directory where program executable resides and
 * determines the installation directory.
 */
_TCHAR* getProgramDir( )
{
	_TCHAR*  ch;
	_TCHAR*  programDir;
	if (program == NULL)
		return NULL;
    programDir = malloc( (_tcslen( program ) + 1) * sizeof(_TCHAR) );
    _tcscpy( programDir, program );
    ch = lastDirSeparator( programDir );
	if (ch != NULL)
    {
    	*(ch+1) = _T_ECLIPSE('\0');
   		return programDir;
    }

    free( programDir );
    return NULL;
}

static _TCHAR* findSplash(_TCHAR* splashArg) {
	struct _stat stats;
	_TCHAR *ch;
	_TCHAR *path, *prefix;
	size_t length;
	
	if (splashArg == NULL)
		return NULL;
	
	splashArg = _tcsdup(splashArg);
	length = _tcslen(splashArg);
	/* _tstat doesn't seem to like dirSeparators on the end */
	while (IS_DIR_SEPARATOR(splashArg[length - 1])) {
		splashArg[--length] = 0;
	}
	
	/* does splashArg exist */
	if (_tstat(splashArg, &stats) == 0) {
		/* pointing to a file */
		if (stats.st_mode & S_IFREG) { 
			/* file, use it*/
			return splashArg;
		} else if (stats.st_mode & S_IFDIR) { 
			/*directory, look for splash.bmp*/
			ch = malloc( (length + 12) * sizeof(_TCHAR));
			_stprintf( ch, _T_ECLIPSE("%s%c%s"), splashArg, dirSeparator, _T_ECLIPSE("splash.bmp") );
			if (_tstat(ch, &stats) == 0 && stats.st_mode & S_IFREG) {
				free(splashArg);
				return ch;
			}
			free(ch);
		}
		free(splashArg);
		return NULL;
	}
	
	/* doesn't exist, separate into path & prefix and look for a /path/prefix_<version> */
	ch = lastDirSeparator( splashArg );
	if (ch != NULL) {
		if (IS_ABSOLUTE(splashArg))
		{	/*absolute path*/
			path = _tcsdup(splashArg);
			path[ch - splashArg] = 0;
		} else {
			/* relative path, prepend with programDir */
			path = malloc( (_tcslen(programDir) + ch - splashArg + 2) * sizeof(_TCHAR));
			*ch = 0;
			_stprintf(path, _T_ECLIPSE("%s%c%s"), programDir, dirSeparator, splashArg);
			*ch = dirSeparator;
		}
		prefix = _tcsdup(ch + 1);
	} else {
		/* No separator, treat splashArg as the prefix and look in the plugins dir */
		path = malloc( (_tcslen(programDir) + 9) * sizeof(_TCHAR));
		_stprintf(path, _T_ECLIPSE("%s%c%s"), programDir, dirSeparator, _T_ECLIPSE("plugins"));
		prefix = _tcsdup(splashArg);
	}
	
	ch = findFile(path, prefix);
	free(path);
	free(prefix);
	free(splashArg);
	if (ch != NULL) {
		path = malloc((_tcslen(ch) + 12) * sizeof(_TCHAR));
		_stprintf( path, _T_ECLIPSE("%s%c%s"), ch, dirSeparator, _T_ECLIPSE("splash.bmp") );
		return path;
	}
	return NULL;
}

static _TCHAR* findStartupJar(){
	_TCHAR * file, *ch;
	_TCHAR * pluginsPath;
	struct _stat stats;
	size_t pathLength, progLength;
	
	if( startupArg != NULL ) {
		/* startup jar was specified on the command line */
		ch = _tcsdup(startupArg);
		/* check path will check relative paths against programDir and workingDir */
		file = checkPath(ch, programDir, 1);
		if(file != ch)
			free(ch);
		/* check existence */
		if (_tstat( file, &stats ) != 0) {
			free(file);
			file = NULL;
		}
		return file;
	}

	progLength = pathLength = _tcslen(programDir);
#ifdef MACOSX
	pathLength += 9;
#endif
	pluginsPath = malloc( (pathLength + 1 + 7 + 1) * sizeof(_TCHAR));
	_tcscpy(pluginsPath, programDir);
	if(!IS_DIR_SEPARATOR(pluginsPath[progLength - 1])) {
		pluginsPath[progLength] = dirSeparator;
		pluginsPath[progLength + 1] = 0;
	}
#ifdef MACOSX
	_tcscat(pluginsPath, _T_ECLIPSE("../../../"));
#endif
	_tcscat(pluginsPath, _T_ECLIPSE("plugins"));
	
	/* equinox startup jar? */	
	file = findFile(pluginsPath, DEFAULT_EQUINOX_STARTUP);
	if(file != NULL)
		return file;
	
	/* old startup.jar? */
	ch = OLD_STARTUP;
	file = checkPath(ch, programDir, 1);
	if (_tstat( file, &stats ) == 0)
		return (file == ch) ? _tcsdup(ch) : file;
		
	return NULL;
}

/* 
 * Return the portion of the vmCommand that should be used for relaunching
 * 
 * The memory allocated for the command array must be freed
 */
static _TCHAR ** getRelaunchCommand( _TCHAR **vmCommand  )
{
	int i = -1, req = 0, begin = -1;
	int idx = 0;
	_TCHAR ** relaunch;
	
	if (vmCommand == NULL) return NULL;
	while(vmCommand[++i] != NULL){
		if ( begin == -1 && _tcsicmp( vmCommand[i], *reqVMarg[req] ) == 0) {
			if(reqVMarg[++req] == NULL || *reqVMarg[req] == NULL){
				begin = i + 1;
			}
		}
	}
	
	relaunch = malloc((1 + i + 1) * sizeof(_TCHAR *));
	relaunch[idx++] = program;
	if(begin == -1) {
		begin = 1;
	}
	for (i = begin; vmCommand[i] != NULL; i++){
		if (_tcsicmp(vmCommand[i], SHOWSPLASH) == 0) {
			/* remove if the next argument is not the bitmap to show */
			if(vmCommand[i + 1] != NULL && vmCommand[i + 1][0] == _T_ECLIPSE('-')) {
				continue;
			}
		} else if(_tcsncmp(vmCommand[i], CLASSPATH_PREFIX, _tcslen(CLASSPATH_PREFIX)) == 0) {
			/* skip -Djava.class.path=... */
			continue;
		}
		relaunch[idx++] = vmCommand[i];
	}
	if(_tcsicmp(relaunch[idx - 1], VMARGS) == 0) 
		relaunch[idx - 1] = NULL;
	relaunch[idx] = NULL;
	return relaunch;
}

#ifdef _WIN32
static void createConsole() {
#ifndef WIN64
#define intptr_t long
#endif
	intptr_t stdHandle;
	int conHandle;
	FILE *fp;
	
	AllocConsole();
	
	/* redirect stdout */
	stdHandle = (intptr_t) GetStdHandle(STD_OUTPUT_HANDLE);
	conHandle = _open_osfhandle(stdHandle, _O_TEXT);
	if (conHandle != -1) {
		fp = _fdopen(conHandle, "w");
		*stdout = *fp;
	}
	
	/* redirect stdin */
	stdHandle = (intptr_t) GetStdHandle(STD_INPUT_HANDLE);
	conHandle = _open_osfhandle(stdHandle, _O_TEXT);
	if (conHandle != -1) {
		fp = _fdopen(conHandle, "r");
		*stdin = *fp;
	}
	
	/* stderr */
	stdHandle = (intptr_t) GetStdHandle(STD_ERROR_HANDLE);
	conHandle = _open_osfhandle(stdHandle, _O_TEXT);
	if (conHandle != -1) {
		fp = _fdopen(conHandle, "r");
		*stderr = *fp;
	}
}

/* Determine if the launcher was the eclipsec.exe or not based on whether we have an attached console.
 * This will only be correct if called before createConsole.
 */
static int isConsoleLauncher() {
	HWND (WINAPI *GetConsoleWindow)();
	void * handle = loadLibrary(_T_ECLIPSE("Kernel32.dll"));
	if (handle != NULL) {
		if ( (GetConsoleWindow = findSymbol(handle, _T_ECLIPSE("GetConsoleWindow"))) != NULL) {
			return GetConsoleWindow() != NULL;
		}
	}
	return 0;
}

static void fixDLLSearchPath() {
#ifdef UNICODE
	_TCHAR* functionName = _T_ECLIPSE("SetDllDirectoryW");
#else
	_TCHAR* functionName = _T_ECLIPSE("SetDllDirectoryA");
#endif

	BOOL (WINAPI *SetDLLDirectory)(LPCTSTR);
	void * handle = loadLibrary(_T_ECLIPSE("Kernel32.dll"));
	if (handle != NULL) {
		if ( (SetDLLDirectory = findSymbol(handle, functionName)) != NULL) {
			SetDLLDirectory(_T_ECLIPSE(""));
		}
	}
}

#endif

/* Set the vm to use based on the given .ee file.
 */
static int vmEEProps(_TCHAR * eeFile, _TCHAR ** msg) {
	if (processEEProps(eeFile) != 0) {
		*msg = _tcsdup(eeFile);
		return -1;
	}
	if (eeLibrary != NULL) {
		jniLib = findVMLibrary(eeLibrary);
		if (jniLib != NULL)
			return LAUNCH_JNI;
	}
		
	if (eeConsole != NULL && (debug || needConsole || consoleLauncher) ) {
		javaVM = findSymlinkCommand(eeConsole, 0);
		if (javaVM != NULL)
			return LAUNCH_EXE;
	}
	
	if (eeExecutable != NULL) {
		javaVM = findSymlinkCommand(eeExecutable, 0);
		if (javaVM != NULL)
			return LAUNCH_EXE;
	}
	
	*msg = _tcsdup(eeFile);
	return -1;
}

/*
 * determine the vm to use.
 * return LAUNCH_JNI for launching with JNI invocation API. jniLib contains the name of the library
 * returh LAUNCH_EXE for execing java, javaVM contains the path to the exe
 * return -1 if problem finding vm, the passed in msg points to the places we looked.  Caller should free
 * this memory.
 */
static int determineVM(_TCHAR** msg) {
	_TCHAR* ch  = NULL;
	_TCHAR* result = NULL;
	_TCHAR* vmSearchPath = NULL;
	_TCHAR* defaultJava = defaultVM; /* default exe to look for */
	int type = 0;
	
#ifdef _WIN32
	if (debug || needConsole || consoleLauncher)
		defaultJava = consoleVM; /* windows will want java.exe for the console, not javaw.exe */
#endif
	
	/* vmName is passed in on command line with -vm */
    if (vmName != NULL) {
    	size_t length = _tcslen(vmName);
    	/* remove the trailing separator */
    	if (vmName[length - 1] == _T_ECLIPSE('/') || vmName[length - 1] == _T_ECLIPSE('\\')) {
    		vmName[length - 1] = 0;
    	}
    	
    	vmName = checkPath(vmName, programDir, 1);
    	type = checkProvidedVMType(vmName);
    	switch (type) {
    	case VM_DIRECTORY:
    		/* vmName is a directory, look for default.ee */
    		ch = malloc((_tcslen(vmName) + 1 + _tcslen(DEFAULT_EE) + 1) * sizeof(_TCHAR));
    		_stprintf( ch, _T_ECLIPSE("%s%c%s"), vmName, dirSeparator, DEFAULT_EE );
    		
    		result = findCommand(ch);
    		free(ch);
    		if (result == NULL) {
    			/* No default.ee file, look for default VM */
    			ch = malloc((_tcslen(vmName) + 1 + _tcslen(defaultJava) + 1) * sizeof(_TCHAR));
    			_stprintf( ch, _T_ECLIPSE("%s%c%s"), vmName, dirSeparator, defaultJava );
    			javaVM = findSymlinkCommand(ch, 0);
    			free(ch);
    			if (javaVM == NULL) {
    				/* No vm executable, look for library */
    				ch = malloc((_tcslen(vmName) + 1 + _tcslen(vmLibrary) + 1) * sizeof(_TCHAR));
    				_stprintf( ch, _T_ECLIPSE("%s%c%s"), vmName, dirSeparator, vmLibrary );
    				jniLib = findVMLibrary(ch);
    				if (jniLib != ch)
    					free(ch);
    				if (jniLib != NULL) {
    					return LAUNCH_JNI;
    				}
    				/* found nothing, return error */
    				*msg = malloc( (3 * (_tcslen(vmName) + 2) + _tcslen(DEFAULT_EE) + _tcslen(defaultJava) + _tcslen(vmLibrary) + 1) * sizeof(_TCHAR));
    				_stprintf( *msg, _T_ECLIPSE("%s%c%s\n%s%c%s\n%s%c%s"), vmName, dirSeparator, DEFAULT_EE, 
    																	   vmName, dirSeparator, defaultJava,
    																	   vmName, dirSeparator, vmLibrary);
    				return -1;
    			}
    			break;
    		}
    		
    		/* else default.ee does exist */
    		vmName = result;
    		/* fall through to VM_EE_PROPS*/
    	case VM_EE_PROPS:
    		return vmEEProps(vmName, msg);
    		
    	case VM_LIBRARY:
    		ch = findCommand(vmName);
    		if(ch != NULL) {
    			jniLib = findVMLibrary(ch);
    			if (ch != jniLib)
    				free(ch);
    			return LAUNCH_JNI;
    		}
    		/* file didn't exist, error */
    		if (firstDirSeparator( vmName ) == NULL) {
    			/* if vmName doesn't contain a dirSeparator, we looked on the path */
    			*msg = malloc((_tcslen(pathMsg) + _tcslen(vmName)) * sizeof(_TCHAR));
    			_stprintf( *msg, pathMsg,vmName );
    		} else {
    			*msg = _tcsdup(vmName);
    		}
    		return -1;
    		
    	default:
    		/*otherwise, assume executable */
    		javaVM = findSymlinkCommand(vmName, 0);
    		if(javaVM != NULL) {
#ifdef MACOSX
    			/* right now, we are always doing JNI on Mac */
    			break; 
#else
    			return LAUNCH_EXE;
#endif
    		}
    		/* file didn't exist, error */
    		if (firstDirSeparator( vmName ) == NULL) {
    			/* if vmName doesn't contain a dirSeparator, we looked on the path */
    			*msg = malloc((_tcslen(pathMsg) + _tcslen(vmName)) * sizeof(_TCHAR));
    			_stprintf( *msg, pathMsg, vmName );
    		} else {
    			*msg = _tcsdup(vmName);
    		}
   			return -1;
    	}
    }
    
    if (vmName == NULL) {
    	/* no vm specified, Try to find the VM shipped with eclipse. */
    	
    	/* look first for default.ee */
    	ch = malloc( (_tcslen( programDir ) + _tcslen( shippedVMDir ) + _tcslen( DEFAULT_EE ) + 1) * sizeof(_TCHAR) );
		_stprintf( ch, _T_ECLIPSE("%s%s%s"), programDir, shippedVMDir, DEFAULT_EE );
		result = findCommand(ch);
		free(ch);
    	if (result != NULL) {
    		type = vmEEProps(result, msg);
    		free(result);
    		return type;
    	}
    	
    	/* then look for java(w).exe */
        ch = malloc( (_tcslen( programDir ) + _tcslen( shippedVMDir ) + _tcslen( defaultJava ) + 10) * sizeof(_TCHAR) );
        _stprintf( ch, _T_ECLIPSE("%s%s%s"), programDir, shippedVMDir, defaultJava );
        vmSearchPath = _tcsdup(ch);
 
        javaVM = findSymlinkCommand( ch, 0 );
        free(ch);
    }
    
    if (javaVM == NULL) {
    	/* vm not found yet, look for one on the search path, but don't resolve symlinks */
    	javaVM = findSymlinkCommand(defaultJava, 0);
    	if (javaVM == NULL) {
    		/* can't find vm, error */
    		ch = malloc( (_tcslen(pathMsg) + _tcslen(defaultJava) + 1) * sizeof(_TCHAR));
    		_stprintf(ch, pathMsg, defaultJava);
    		
    		if(vmSearchPath != NULL) {
    			*msg = malloc((_tcslen(ch) + 1 + _tcslen(vmSearchPath) + 1) * sizeof(_TCHAR));
    			_stprintf(*msg, _T_ECLIPSE("%s\n%s"), vmSearchPath, ch);
    			free(ch);
    		} else {
    			*msg = ch;
    		}
    		return -1;
    	}
    }

    if (vmSearchPath != NULL)
    	free(vmSearchPath);
    
#ifndef DEFAULT_JAVA_EXEC
	/* resolve symlinks for finding the library */
	ch = resolveSymlinks(javaVM);
    jniLib = findVMLibrary(ch);
    if (ch != jniLib && ch != javaVM)
		free(ch);
    if (jniLib != NULL) 
    	return LAUNCH_JNI;
#endif
    
    return LAUNCH_EXE;
}

static int processEEProps(_TCHAR* eeFile) 
{
	_TCHAR ** argv;
	_TCHAR * c1, * c2;
	_TCHAR * eeDir;
	int argc;
	int index, i;
	int matches = 0;
	Option *option;
	
	if(readConfigFile(eeFile, &argc, &argv) != 0)
		return -1;

	nEEargs = argc;
	eeVMarg = argv;
	
	eeDir = _tcsdup(eeFile);
	c1 = lastDirSeparator( eeDir );
	while (c1 != NULL)
    {
    	*c1 = _T_ECLIPSE('\0');
    	c1--; /* IS_DIR_SEPARATOR evalutes c twice, decrement out here */
    	if (!IS_DIR_SEPARATOR(*c1))
    		c1 = NULL;
    }
	
    for (index = 0; index < argc; index++){
    	/* replace ${ee.home} with eeDir, loop in case there is more than one per argument */
    	while( (c1 = _tcsstr(argv[index], EE_HOME_VAR)) != NULL)
    	{
    		/* the space needed for c1 is included in _tcslen(argv[index]) */
    		c2 = malloc( (_tcslen(argv[index]) + _tcslen(eeDir) + 1) * sizeof(_TCHAR));
    		*c1 = _T_ECLIPSE('\0');
    		_stprintf(c2, _T_ECLIPSE("%s%s%s"), argv[index], eeDir, c1 + 10); /* ${ee.home} is 10 characters */
    		free(argv[index]);
    		argv[index] = c2;
    	}
    	
    	/* Find the corresponding argument is a option supported by the launcher */
        option = NULL;
        for (i = 0; option == NULL && i < eeOptionsSize; i++)
        {
        	if (_tcsncmp( argv[index], eeOptions[i].name, _tcslen(eeOptions[i].name) ) == 0) {
        	    option = &eeOptions[i];
        	    break;
        	}
       	}
        if(option != NULL) {
        	++matches;	
        	if (option->flag & VALUE_IS_FLAG)
        		*((int*)option->value) = 1;
        	else {
        		c1 = malloc( (_tcslen(argv[index]) - _tcslen(option->name) + 1) *sizeof(_TCHAR));
            	_tcscpy(c1, argv[index] + _tcslen(option->name));
        		if (option->flag & ADJUST_PATH && option->flag & VALUE_IS_LIST) {
        			c2 = checkPathList(c1, eeDir, 1);
       				free(c1);
    				c1 = c2;
        		} else if (option->flag & ADJUST_PATH) {
        			c2 = checkPath(c1, eeDir, 1);
        			if (c2 != c1) {
        				free(c1);
        				c1 = c2;
        			}
        		}
    			*((_TCHAR**)option->value) = c1;
        	}
        	if(matches == eeOptionsSize)
        		break;
        }
    }
    /* set ee.home, ee.filename variables, and NULL */
    argv = realloc(argv, (nEEargs + 3) * sizeof(_TCHAR*));
    
    c1 = malloc( (_tcslen(EE_HOME) + _tcslen(eeDir) + 1) * sizeof(_TCHAR));
    _stprintf(c1, _T_ECLIPSE("%s%s"), EE_HOME, eeDir);
    argv[nEEargs++] = c1;
    
    c1 = malloc( (_tcslen(EE_FILENAME) + _tcslen(eeFile) + 1) * sizeof(_TCHAR));
    _stprintf(c1, _T_ECLIPSE("%s%s"), EE_FILENAME, eeFile);
    argv[nEEargs++] = c1;
    
    argv[nEEargs] = NULL;
    
    free(eeDir);
    return 0;
}

/* returns an array of paths that should be place on the search path for loading
 * the vm shared libraries.
 * Each entry is terminated with the platform path separator.
 * Entries are either from the ee.library.path or calculated from the path to the
 * vm shared library itself.
 */
_TCHAR ** getVMLibrarySearchPath(_TCHAR * vmLibrary) {
	_TCHAR ** paths = NULL;
	_TCHAR * buffer = NULL;
	_TCHAR * path, * entry, *c;
	_TCHAR separator;
	int numPaths = 3;
	int i;
	struct _stat stats;
	
	buffer = (eeLibPath != NULL) ? _tcsdup(eeLibPath) : _tcsdup(vmLibrary);	
#ifdef WIN32
	/* On windows we sometimes get '/' instead of '\', just always use '/'  */
	i = -1;
	while (buffer[++i] != 0) {
		if (buffer[i] == _T_ECLIPSE('\\')) 
			buffer[i] = _T_ECLIPSE('/');
	}
#endif
	
	separator = (eeLibPath != NULL) ? pathSeparator : _T_ECLIPSE('/');
	
	if (eeLibPath != NULL) {
		/* count number of path elements */
		numPaths = 1;
		c = eeLibPath;
		while( (c = _tcschr(c, pathSeparator)) != NULL) {
			numPaths++;
			c++;
		}
	} 
		
	paths = malloc((numPaths + 1) * sizeof(_TCHAR*));
	paths[numPaths] = NULL;
		
	/* We are either splitting eeLibPath (eg path1:path2), or we are extracting
	 * from libPath where we want the directory containing the library and the
	 * parent directory of that, and also grandparent/lib/arch */
	for (i = 0; i < numPaths; i++) {
		c = _tcsrchr(buffer, separator);
		if (c != 0) {
			*c++ = 0;
			if (eeLibPath != NULL) { 
				path = c;		/* we want from c to the end */
			} else { 
				path = buffer; 	/* we want from the start to c */
			}
		} else {
			if (eeLibPath != NULL) {
				path = buffer;
			} else {
				paths[i] = NULL;
				break;
			}
		}
		if (path != NULL) {
			entry = resolveSymlinks(path); /* this may be a new string */
			if (eeLibPath == NULL && i == 2) {
				/* trying grandparent/lib/arch */
				const _TCHAR * arch = getVMArch();
				paths[i] = malloc((_tcslen(entry) + 7 + _tcslen(arch)) * sizeof(_TCHAR));
				_stprintf(paths[i], _T_ECLIPSE("%s/lib/%s"), entry, arch);
				/* only add if the path actually exists */
				if (_tstat(paths[i], &stats) == 0) {
					_TCHAR separatorString[] = { pathSeparator, 0 };
					_tcscat(paths[i], separatorString);
				} else {
					free(paths[i]);
					paths[i] = NULL;
				}
			} else {
				paths[i] = malloc((_tcslen(entry) + 2) * sizeof(_TCHAR));
				_stprintf( paths[i], _T_ECLIPSE("%s%c"), entry, pathSeparator );
			}
			if (entry != path)
				free(entry);
			path = NULL;
		}
	}
	
	free(buffer);
	return paths;
}

/* translate the osArchArg into the value that we expect the jre to use */
const _TCHAR* getVMArch() {
	if (_tcscmp(osArchArg, _T_ECLIPSE("x86_64")) == 0)
		return _T_ECLIPSE("amd64");
	else if (_tcscmp(osArchArg, _T_ECLIPSE("x86")) == 0)
		return _T_ECLIPSE("i386");
	else
		return osArchArg;
}
