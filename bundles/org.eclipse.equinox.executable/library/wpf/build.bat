@rem *******************************************************************************
@rem  Copyright (c) 2000, 2009 IBM Corporation and others.
@rem  All rights reserved. This program and the accompanying materials
@rem  are made available under the terms of the Eclipse Public License v1.0
@rem  which accompanies this distribution, and is available at 
@rem  http://www.eclipse.org/legal/epl-v10.html
@rem  
@rem  Contributors:
@rem      IBM Corporation - initial API and implementation
@rem      Kevin Cornell (Rational Software Corporation)
@rem ********************************************************************** 
@rem 
@rem  Usage: sh build.sh [<optional switches>] [clean]
@rem 
@rem    where the optional switches are:
@rem        -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
@rem        -library <PROGRAM_LIBRARY>- dll filename (eclipse.dll)
@rem        -os     <DEFAULT_OS>      - default Eclipse "-os" value (qnx) 
@rem        -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value (x86) 
@rem        -ws     <DEFAULT_WS>      - default Eclipse "-ws" value (photon)
@rem		-java   <JAVA_HOME>       - location of a Java SDK for JNI headers 
@rem 
@rem 
@rem     This script can also be invoked with the "clean" argument.
@rem
@rem NOTE: The C compiler needs to be setup. This script has been
@rem       tested against Microsoft Visual C and C++ Compiler 6.0.
@rem	
@rem Uncomment the lines below and edit MSVC_HOME to point to the
@rem correct root directory of the compiler installation, if you
@rem want this to be done by this script.
@rem 
@rem ******
@echo off

IF x.%1==x.x86 shift

rem *****
rem Javah
rem *****
IF x."%JAVA_HOME%"==x. set JAVA_HOME="S:\swt-builddir\ibm-java2-sdk-50-win-i386"
set javaHome=%JAVA_HOME%

:MSVC

call "S:\swt-builddir\MSSDKs\Microsoft SDK 6.0 Vista\Bin\setenv.cmd" /x86 /vista
:MAKE

rem --------------------------
rem Define default values for environment variables used in the makefiles.
rem --------------------------
set programOutput=eclipse.exe
set programLibrary=eclipse.dll
set defaultOS=win32
set defaultOSArch=x86
set defaultWS=wpf
set makefile=make_wpf.mak
set OS=Windows

rem --------------------------
rem Parse the command line arguments and override the default values.
rem --------------------------
set extraArgs=
:WHILE
if "%1" == "" goto WHILE_END
    if "%2" == ""       goto LAST_ARG

    if "%1" == "-os" (
		set defaultOS=%2
		shift
		goto NEXT )
    if "%1" == "-arch" (
		set defaultOSArch=%2
		shift
		goto NEXT )
    if "%1" == "-ws" (
		set defaultWS=%2
		shift
		goto NEXT )
    if "%1" == "-output" (
		set programOutput=%2
		shift
		goto NEXT )
	if "%1" == "-library" (
		set programLibrary=%2
		shift
		goto NEXT )
	if "%1" == "-java" (
		set javaHome=%2
		shift
		goto NEXT )
:LAST_ARG
        set extraArgs=%extraArgs% %1

:NEXT
    shift
    goto WHILE
:WHILE_END

rem --------------------------
rem Set up environment variables needed by the makefile.
rem --------------------------
set PROGRAM_OUTPUT=%programOutput%
set PROGRAM_LIBRARY=%programLibrary%
set DEFAULT_OS=%defaultOS%
set DEFAULT_OS_ARCH=%defaultOSArch%
set DEFAULT_WS=%defaultWS%
set OUTPUT_DIR=..\..\bin\%defaultWS%\%defaultOS%\%defaultOSArch%
set JAVA_HOME=%javaHome%

rem --------------------------
rem Run nmake to build the executable.
rem --------------------------
if "%extraArgs%" == "" goto MAKE_ALL

nmake -f %makefile% %extraArgs%
goto DONE

:MAKE_ALL
echo Building %OS% launcher. Defaults: -os %DEFAULT_OS% -arch %DEFAULT_OS_ARCH% -ws %DEFAULT_WS%
nmake -f %makefile% clean
nmake -f %makefile% %1 %2 %3 %4
goto DONE


:DONE
